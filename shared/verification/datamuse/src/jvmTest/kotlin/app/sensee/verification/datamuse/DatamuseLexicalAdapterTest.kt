package app.sensee.verification.datamuse

import app.sensee.verification.core.LexicalExistence
import app.sensee.verification.core.LexicalVerificationQuery
import app.sensee.verification.core.NormalizationKind
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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatamuseLexicalAdapterTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `an exact match confirms the term without a normalization candidate`() {
        val engine =
            MockEngine {
                respond(
                    content = """[{"word":"hello","score":150000}]""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            }

        val result = runBlocking { adapter(engine).lookup(query("hello")) }

        assertEquals(VerifierAvailability.Available, result.availability)
        assertEquals(LexicalExistence.Confirmed, result.existence)
        assertTrue(result.normalized.candidates.isEmpty())
    }

    @Test
    fun `a strong spell suggestion produces a SpellFix normalization candidate`() {
        val engine =
            MockEngine {
                respond(
                    content = """[{"word":"hello","score":120000}]""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            }

        val result = runBlocking { adapter(engine).lookup(query("helo")) }

        // Top hit differs from input AND clears the strong-suggestion
        // threshold: we surface NotFound + a SpellFix candidate so the
        // aggregator can show "did you mean hello".
        assertEquals(LexicalExistence.NotFound, result.existence)
        val candidate = assertNotNull(result.normalized.candidates.firstOrNull())
        assertEquals("hello", candidate.text)
        assertEquals(NormalizationKind.SpellFix, candidate.kind)
    }

    @Test
    fun `a weak suggestion stays silent so it cannot override a primary dictionary`() {
        val engine =
            MockEngine {
                respond(
                    content = """[{"word":"helot","score":10}]""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            }

        val result = runBlocking { adapter(engine).lookup(query("helo")) }

        // Below the strong threshold: Unknown existence, no normalization;
        // a primary dictionary's evidence still wins in the aggregator.
        assertEquals(LexicalExistence.Unknown, result.existence)
        assertTrue(result.normalized.candidates.isEmpty())
    }

    @Test
    fun `an empty response degrades instead of forging a NotFound`() {
        val engine =
            MockEngine {
                respond(
                    content = "[]",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            }

        val result = runBlocking { adapter(engine).lookup(query("xyzqq")) }

        assertTrue(result.availability is VerifierAvailability.Degraded)
    }

    @Test
    fun `a 5xx degrades without leaking the exception`() {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }

        val result = runBlocking { adapter(engine).lookup(query("hello")) }

        assertTrue(result.availability is VerifierAvailability.Degraded)
    }

    @Test
    fun `a multi-word input is rejected without a network round trip`() {
        var calls = 0
        val engine =
            MockEngine {
                calls++
                respond(content = "[]", status = HttpStatusCode.OK)
            }

        val result = runBlocking { adapter(engine).lookup(query("come across")) }

        assertEquals(0, calls)
        assertTrue(result.availability is VerifierAvailability.Unavailable)
    }

    @Test
    fun `disabled network short-circuits without contacting the service`() {
        var calls = 0
        val engine =
            MockEngine {
                calls++
                respond(content = "[]", status = HttpStatusCode.OK)
            }
        val offline =
            LexicalVerificationQuery(
                text = "hello",
                studyLanguageTag = "en",
                policy = VerificationPolicy(allowNetwork = false),
            )

        val result = runBlocking { adapter(engine).lookup(offline) }

        assertEquals(0, calls)
        assertTrue(result.availability is VerifierAvailability.Unavailable)
    }

    private fun adapter(engine: MockEngine): DatamuseLexicalAdapter =
        DatamuseLexicalAdapter(
            httpClient =
                HttpClient(engine) {
                    expectSuccess = true
                    install(ContentNegotiation) { json(json) }
                },
            clock = { 0L },
            baseUrl = "https://api.datamuse.com/words",
        )

    private fun query(text: String): LexicalVerificationQuery =
        LexicalVerificationQuery(text = text, studyLanguageTag = "en")
}
