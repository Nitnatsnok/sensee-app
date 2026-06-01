package app.sensee.verification.languagetool

import app.sensee.verification.core.ExampleCheckRequest
import app.sensee.verification.core.ExampleLocation
import app.sensee.verification.core.FindingSeverity
import app.sensee.verification.core.SentenceHint
import app.sensee.verification.core.VerificationPolicy
import app.sensee.verification.core.VerifierAvailability
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LanguageToolExampleCheckerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a match inside one segment lands as WithinSegment with a segment-local range`() =
        runTest {
            // "She comes acros as confident." — 'acros' typo in a Text segment.
            val sentence =
                SentenceHint(
                    listOf(
                        SentenceHint.Segment.Text("She "),
                        SentenceHint.Segment.Target("comes"),
                        SentenceHint.Segment.Text(" acros as confident."),
                    ),
                )
            // Offset of "acros" in plainText() is 10, length 5.
            val engine =
                MockEngine {
                    respond(
                        content =
                            """
                            {"matches":[{"message":"Possible spelling mistake","offset":10,
                            "length":5,"replacements":[{"value":"across"}],
                            "rule":{"id":"MORFOLOGIK_RULE_EN_US","issueType":"misspelling"}}]}
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }
            val checker = checker(engine)

            val result = checker.check(ExampleCheckRequest(sentence, "en"))

            assertEquals(VerifierAvailability.Available, result.availability)
            val issue = result.issues.single()
            assertEquals(FindingSeverity.Error, issue.severity)
            val location = issue.location
            assertTrue(location is ExampleLocation.WithinSegment)
            // The typo `acros` starts at plain offset 10, segment 2 starts at 9
            // ("She " = 4, "comes" = 5 → cumulative 9), so relative start is 1.
            assertEquals(2, location.segmentIndex)
            assertEquals(1..5, location.range)
            // Rewrite always re-annotates; do not lose Target topology silently.
            val rewrite = assertNotNull(result.rewrite)
            assertTrue(rewrite.requiresTargetReannotation)
        }

    @Test
    fun `a match straddling segments degrades to PlainTextSpan`() =
        runTest {
            val sentence =
                SentenceHint(
                    listOf(
                        SentenceHint.Segment.Text("She "),
                        SentenceHint.Segment.Target("come"),
                        SentenceHint.Segment.Text(" across."),
                    ),
                )
            // Hypothetical match covering parts of two segments (8..14):
            // "come ac" inside segments 1 and 2.
            val engine =
                MockEngine {
                    respond(
                        content =
                            """
                            {"matches":[{"message":"x","offset":4,"length":8,
                            "replacements":[],"rule":{"id":"X","issueType":"style"}}]}
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }
            val checker = checker(engine)

            val result = checker.check(ExampleCheckRequest(sentence, "en"))

            val location = result.issues.single().location
            assertTrue(location is ExampleLocation.PlainTextSpan)
        }

    @Test
    fun `no matches yields Available with an empty issues list`() =
        runTest {
            val engine =
                MockEngine {
                    respond(
                        content = """{"matches":[]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }
            val checker = checker(engine)

            val result =
                checker.check(
                    ExampleCheckRequest(
                        SentenceHint(listOf(SentenceHint.Segment.Text("All good."))),
                        "en",
                    ),
                )

            assertEquals(VerifierAvailability.Available, result.availability)
            assertTrue(result.issues.isEmpty())
            assertEquals(null, result.rewrite)
        }

    @Test
    fun `a 5xx degrades without leaking the exception`() =
        runTest {
            val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
            val checker = checker(engine)

            val result =
                checker.check(
                    ExampleCheckRequest(
                        SentenceHint(listOf(SentenceHint.Segment.Text("hello"))),
                        "en",
                    ),
                )

            assertTrue(result.availability is VerifierAvailability.Degraded)
        }

    @Test
    fun `network-disabled policy returns Unavailable without sending the sentence`() =
        runTest {
            var calls = 0
            val engine =
                MockEngine {
                    calls += 1
                    respondError(HttpStatusCode.InternalServerError)
                }
            val checker = checker(engine)

            val result =
                checker.check(
                    ExampleCheckRequest(
                        sentence = SentenceHint(listOf(SentenceHint.Segment.Text("hello"))),
                        studyLanguageTag = "en",
                        policy = VerificationPolicy(allowNetwork = false),
                    ),
                )

            assertTrue(result.availability is VerifierAvailability.Unavailable)
            assertEquals(0, calls)
        }

    @Test
    fun `a negative offset is clamped and never produces a reversed IntRange`() =
        runTest {
            // I5: malformed LT response with a negative offset would otherwise
            // create a span `-2 until 1` and later subscript downstream code
            // out of bounds. Adapter must clamp into the plain-text range; a
            // collapsed span degrades to WholeSentence rather than an
            // out-of-bounds WithinSegment.
            val engine =
                MockEngine {
                    respond(
                        content =
                            """
                            {"matches":[{"message":"x","offset":-2,"length":3,
                            "replacements":[],"rule":{"id":"X","issueType":"style"}}]}
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }
            val checker = checker(engine)

            val result =
                checker.check(
                    ExampleCheckRequest(
                        SentenceHint(listOf(SentenceHint.Segment.Text("She comes."))),
                        "en",
                    ),
                )

            val location = result.issues.single().location
            assertTrue(
                location is ExampleLocation.WithinSegment || location is ExampleLocation.WholeSentence,
                "negative offset must not surface as a raw out-of-bounds range; got $location",
            )
            if (location is ExampleLocation.WithinSegment) {
                assertTrue(location.range.first >= 0)
                assertTrue(location.range.last >= location.range.first)
            }
        }

    @Test
    fun `an offset beyond the text length collapses to WholeSentence`() =
        runTest {
            val engine =
                MockEngine {
                    respond(
                        content =
                            """
                            {"matches":[{"message":"x","offset":500,"length":3,
                            "replacements":[],"rule":{"id":"X","issueType":"style"}}]}
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }
            val checker = checker(engine)

            val result =
                checker.check(
                    ExampleCheckRequest(
                        SentenceHint(listOf(SentenceHint.Segment.Text("She comes."))),
                        "en",
                    ),
                )

            assertEquals(ExampleLocation.WholeSentence, result.issues.single().location)
        }

    @Test
    fun `a length running past the end is clamped to the text boundary`() =
        runTest {
            val engine =
                MockEngine {
                    respond(
                        content =
                            """
                            {"matches":[{"message":"x","offset":4,"length":99,
                            "replacements":[],"rule":{"id":"X","issueType":"style"}}]}
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }
            val checker = checker(engine)

            val result =
                checker.check(
                    ExampleCheckRequest(
                        SentenceHint(listOf(SentenceHint.Segment.Text("She comes."))),
                        "en",
                    ),
                )

            val location = result.issues.single().location
            // Full plain text is 10 chars: "She comes." — single segment, so
            // the safeEnd=10, span=4 until 10 → segment-relative range 4..9.
            // The strong assertion is "range never reaches into out-of-bounds
            // territory"; the exact range may differ if the adapter ever
            // changes the WithinSegment vs PlainTextSpan policy.
            assertTrue(
                location is ExampleLocation.WithinSegment || location is ExampleLocation.PlainTextSpan,
                "clamped match must land somewhere; got $location",
            )
            val absoluteRange =
                when (location) {
                    is ExampleLocation.WithinSegment -> location.range
                    is ExampleLocation.PlainTextSpan -> location.range
                    is ExampleLocation.Segment, is ExampleLocation.WholeSentence ->
                        error("clamped match must not collapse to whole-sentence here")
                }
            assertTrue(absoluteRange.first >= 0)
            assertTrue(
                absoluteRange.last <= 9,
                "clamped end must stay within text length 10, got ${absoluteRange.last}",
            )
            assertTrue(absoluteRange.last >= absoluteRange.first, "range must not be reversed")
        }

    @Test
    fun `a zero-length insertion match attaches to the segment that starts at the caret`() =
        runTest {
            // The plain text is "She comes." → segments are [0..3] "She ",
            // [4..8] "comes", [9..9] ".". A zero-length match at offset 4
            // (caret between "She " and "comes") must land on segment 1, the
            // one that STARTS at the caret. The pre-fix code attached it to
            // segment 0 because its end == caret - 1.
            val sentence =
                SentenceHint(
                    listOf(
                        SentenceHint.Segment.Text("She "),
                        SentenceHint.Segment.Target("comes"),
                        SentenceHint.Segment.Text("."),
                    ),
                )
            val engine =
                MockEngine {
                    respond(
                        content =
                            """
                            {"matches":[{"message":"missing word","offset":4,"length":0,
                            "replacements":[],"rule":{"id":"INS","issueType":"grammar"}}]}
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }
            val checker = checker(engine)

            val result = checker.check(ExampleCheckRequest(sentence, "en"))

            val location = result.issues.single().location
            assertTrue(location is ExampleLocation.WithinSegment)
            assertEquals(1, location.segmentIndex)
            assertEquals(0..0, location.range)
        }

    @Test
    fun `a partial-replacement set drops the auto-rewrite entirely`() =
        runTest {
            val engine =
                MockEngine {
                    respond(
                        content =
                            """
                            {"matches":[
                              {"message":"a","offset":0,"length":3,"replacements":[{"value":"AAA"}],
                               "rule":{"id":"A","issueType":"misspelling"}},
                              {"message":"b","offset":5,"length":2,"replacements":[],
                               "rule":{"id":"B","issueType":"misspelling"}}
                            ]}
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }
            val checker = checker(engine)

            val result =
                checker.check(
                    ExampleCheckRequest(
                        SentenceHint(listOf(SentenceHint.Segment.Text("foo bar baz"))),
                        "en",
                    ),
                )

            // One match has no replacement — applying only the other leaves a
            // half-fixed sentence which is worse than no rewrite at all.
            assertEquals(null, result.rewrite)
            assertEquals(2, result.issues.size)
        }

    private fun checker(engine: MockEngine): LanguageToolExampleChecker =
        LanguageToolExampleChecker(
            httpClient =
                HttpClient(engine) {
                    expectSuccess = true
                    install(ContentNegotiation) { json(json) }
                },
            clock = { 0L },
            baseUrl = "https://api.languagetool.org/v2",
        )
}
