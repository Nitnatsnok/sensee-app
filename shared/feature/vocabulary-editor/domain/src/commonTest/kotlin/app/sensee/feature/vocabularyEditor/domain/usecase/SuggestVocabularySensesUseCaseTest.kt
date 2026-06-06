package app.sensee.feature.vocabularyEditor.domain.usecase

import app.sensee.ai.core.contract.AiEnrichmentClient
import app.sensee.ai.core.contract.EnrichmentAvailability
import app.sensee.ai.core.contract.EnrichmentResult
import app.sensee.ai.core.model.EnrichmentExample
import app.sensee.ai.core.model.EnrichmentSuggestion
import app.sensee.ai.core.request.EnrichmentRequest
import app.sensee.ai.core.request.SenseCoverage
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import app.sensee.verification.core.contract.ExampleCheckRequest
import app.sensee.verification.core.contract.ExampleCheckResult
import app.sensee.verification.core.contract.ExampleIssue
import app.sensee.verification.core.contract.ExampleLocation
import app.sensee.verification.core.contract.ExampleQualityChecker
import app.sensee.verification.core.contract.FindingSeverity
import app.sensee.verification.core.contract.LexicalVerificationQuery
import app.sensee.verification.core.contract.LexicalVerificationReport
import app.sensee.verification.core.contract.LexicalVerifier
import app.sensee.verification.core.contract.VerifierAvailability
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SuggestVocabularySensesUseCaseTest {
    private class FakeAi(
        private val result: EnrichmentResult,
    ) : AiEnrichmentClient {
        override suspend fun enrich(request: EnrichmentRequest): EnrichmentResult = result
    }

    private val noTaxonomyProvider = TaxonomyInvariantsProvider { null }

    private object UnavailableVerifier : LexicalVerifier {
        override suspend fun verify(query: LexicalVerificationQuery): LexicalVerificationReport =
            LexicalVerificationReport.unavailable("test-only: no verifier wired")
    }

    private object NoOpExampleQualityChecker : ExampleQualityChecker {
        override suspend fun check(request: ExampleCheckRequest): ExampleCheckResult =
            ExampleCheckResult(VerifierAvailability.Available, emptyList(), rewrite = null, sources = emptyList())
    }

    private class FlaggingExampleChecker(
        private val blockedSubstrings: Set<String>,
    ) : ExampleQualityChecker {
        override suspend fun check(request: ExampleCheckRequest): ExampleCheckResult {
            val flagged = blockedSubstrings.any { request.sentence.plainText().contains(it) }
            val issues =
                if (flagged) {
                    listOf(
                        ExampleIssue(
                            code = "WEAK",
                            severity = FindingSeverity.Error,
                            location = ExampleLocation.WholeSentence,
                            message = "weak example",
                            sources = emptyList(),
                        ),
                    )
                } else {
                    emptyList()
                }
            return ExampleCheckResult(VerifierAvailability.Available, issues, rewrite = null, sources = emptyList())
        }
    }

    private class RecordingAi(
        private val result: EnrichmentResult,
    ) : AiEnrichmentClient {
        var lastRequest: EnrichmentRequest? = null
            private set

        override suspend fun enrich(request: EnrichmentRequest): EnrichmentResult {
            lastRequest = request
            return result
        }
    }

    private class RecordingVerifier : LexicalVerifier {
        var lastQuery: LexicalVerificationQuery? = null
            private set

        override suspend fun verify(query: LexicalVerificationQuery): LexicalVerificationReport {
            lastQuery = query
            return LexicalVerificationReport.unavailable("test-only: no verifier wired")
        }
    }

    private fun available(vararg translations: String) =
        EnrichmentResult(
            availability = EnrichmentAvailability.Available,
            suggestions =
                translations.map {
                    EnrichmentSuggestion(
                        translation = it,
                        examples = listOf(EnrichmentExample(sentence = "I [[$it]] it.")),
                    )
                },
        )

    private fun useCase(
        ai: AiEnrichmentClient,
        verifier: LexicalVerifier = UnavailableVerifier,
        checker: ExampleQualityChecker = NoOpExampleQualityChecker,
    ) = SuggestVocabularySensesUseCase(
        ai,
        verifier,
        setOf(checker),
        noTaxonomyProvider,
        noOpAppDiagnostics(),
    )

    @Test
    fun `enrichment suggestions map into senses`() =
        runTest {
            val result = useCase(FakeAi(available("наткнуться", "произвести впечатление")))(term = "come across")

            assertEquals(
                listOf("наткнуться", "произвести впечатление"),
                result.senses.map { it.translation },
            )
            assertEquals(EnrichmentAvailability.Available, result.availability)
        }

    @Test
    fun `a suggestion the provider returns with no example is dropped`() =
        runTest {
            val ai =
                FakeAi(
                    EnrichmentResult(
                        availability = EnrichmentAvailability.Available,
                        suggestions =
                            listOf(
                                EnrichmentSuggestion(
                                    translation = "наткнуться",
                                    examples = listOf(EnrichmentExample(sentence = "I [[came across]] it.")),
                                ),
                                EnrichmentSuggestion(translation = "без примера"),
                            ),
                    ),
                )

            val senses = useCase(ai)(term = "come across").senses

            assertEquals(
                listOf("наткнуться"),
                senses.map { it.translation },
                "an example-less suggestion never becomes a selectable candidate",
            )
        }

    @Test
    fun `unavailable enrichment surfaces no senses`() =
        runTest {
            val result =
                useCase(FakeAi(EnrichmentResult(EnrichmentAvailability.Unavailable("no key"))))(term = "come across")

            assertTrue(result.senses.isEmpty())
            assertTrue(result.availability is EnrichmentAvailability.Unavailable)
        }

    @Test
    fun `an unavailable verifier means the AI request rides with no grounding attached`() =
        runTest {
            val ai = RecordingAi(available("наткнуться"))

            useCase(ai, verifier = UnavailableVerifier)(term = "come across")

            assertNull(ai.lastRequest?.grounding, "Unavailable verifier must not contribute grounding")
        }

    @Test
    fun `a license-permissive verifier feeds grounding into the AI request`() =
        runTest {
            val ai = RecordingAi(available("наткнуться"))

            useCase(ai, verifier = CuratedVerifier)(term = "come across")

            val grounding = ai.lastRequest?.grounding
            assertEquals("come", grounding?.lemma)
            assertEquals("phrasal_verb", grounding?.entryType)
            assertEquals("come", grounding?.unit?.headLemma)
            assertEquals(listOf("test-fixture"), grounding?.sources?.map { it.sourceId })
        }

    @Test
    fun `language tags are passed to verifier and enrichment request`() =
        runTest {
            val ai = RecordingAi(available("наткнуться"))
            val verifier = RecordingVerifier()

            useCase(ai, verifier = verifier)(
                term = "come across",
                studyLanguageTag = "en-GB",
                nativeLanguageTag = "ru-RU",
            )

            assertEquals("en-GB", verifier.lastQuery?.studyLanguageTag)
            assertEquals("en-GB", ai.lastRequest?.studyLanguageTag)
            assertEquals("ru-RU", ai.lastRequest?.nativeLanguageTag)
        }

    @Test
    fun `grounding is dropped when the only source forbids LLM context`() =
        runTest {
            val ai = RecordingAi(available("наткнуться"))

            useCase(ai, verifier = RestrictedVerifier)(term = "come across")

            assertNull(
                ai.lastRequest?.grounding,
                "A source with usableAsLlmContext=false must not leak into the LLM prompt",
            )
        }

    @Test
    fun `source-less family facts are not sent as LLM grounding`() =
        runTest {
            val ai = RecordingAi(available("наткнуться"))

            useCase(ai, verifier = SourceLessFamilyVerifier)(term = "come across")

            val grounding = ai.lastRequest?.grounding
            assertEquals("phrasal_verb", grounding?.entryType)
            assertNull(grounding?.lemma)
            assertNull(grounding?.unit)
            assertNull(grounding?.family)
        }

    @Test
    fun `manual completion uses minimal coverage with verifier grounding`() =
        runTest {
            val ai = RecordingAi(available("дополненный смысл"))

            val result =
                useCase(ai, verifier = CuratedVerifier).completeManualSense(
                    term = "come across",
                    userNote = "мой черновик",
                )

            assertEquals(listOf("дополненный смысл"), result.map { it.translation })
            assertEquals(SenseCoverage.Minimal, ai.lastRequest?.senseCoverage)
            assertEquals("мой черновик", ai.lastRequest?.userNote)
            assertEquals("phrasal_verb", ai.lastRequest?.grounding?.entryType)
        }

    @Test
    fun `the example-quality filter drops a flagged example but keeps the others`() =
        runTest {
            val ai =
                FakeAi(
                    EnrichmentResult(
                        availability = EnrichmentAvailability.Available,
                        suggestions =
                            listOf(
                                EnrichmentSuggestion(
                                    translation = "наткнуться",
                                    examples =
                                        listOf(
                                            EnrichmentExample(sentence = "I [[came across]] a good example."),
                                            EnrichmentExample(sentence = "I [[came across]] a bad example."),
                                        ),
                                ),
                            ),
                    ),
                )

            val sense =
                useCase(ai, checker = FlaggingExampleChecker(blockedSubstrings = setOf("bad")))(term = "come across")
                    .senses
                    .single()

            assertEquals(1, sense.contextualApplications.size)
            assertTrue(
                sense.contextualApplications
                    .single()
                    .sentence
                    .marked()
                    .contains("good"),
            )
        }

    @Test
    fun `the example-quality filter never empties the example list`() =
        runTest {
            val ai =
                FakeAi(
                    EnrichmentResult(
                        availability = EnrichmentAvailability.Available,
                        suggestions =
                            listOf(
                                EnrichmentSuggestion(
                                    translation = "наткнуться",
                                    examples =
                                        listOf(
                                            EnrichmentExample(sentence = "I [[came across]] a bad one."),
                                            EnrichmentExample(sentence = "Another bad [[example]]."),
                                        ),
                                ),
                            ),
                    ),
                )

            val sense =
                useCase(ai, checker = FlaggingExampleChecker(blockedSubstrings = setOf("bad")))(term = "come across")
                    .senses
                    .single()

            // Both examples are flagged, but the filter must keep at least one.
            assertEquals(1, sense.contextualApplications.size)
        }

    private object CuratedVerifier : LexicalVerifier {
        override suspend fun verify(query: LexicalVerificationQuery): LexicalVerificationReport =
            VerifierReports.simplePhrasalVerb(usableAsLlmContext = true)
    }

    private object SourceLessFamilyVerifier : LexicalVerifier {
        override suspend fun verify(query: LexicalVerificationQuery): LexicalVerificationReport =
            VerifierReports.simplePhrasalVerb(
                usableAsLlmContext = true,
                includeFamilySources = false,
            )
    }

    private object RestrictedVerifier : LexicalVerifier {
        override suspend fun verify(query: LexicalVerificationQuery): LexicalVerificationReport =
            VerifierReports.simplePhrasalVerb(usableAsLlmContext = false)
    }
}
