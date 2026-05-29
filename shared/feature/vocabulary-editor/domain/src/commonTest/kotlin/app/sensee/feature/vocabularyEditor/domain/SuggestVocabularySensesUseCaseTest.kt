package app.sensee.feature.vocabularyEditor.domain

import app.sensee.ai.core.AiEnrichmentClient
import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.core.EnrichmentSuggestion
import app.sensee.ai.core.SenseCoverage
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import app.sensee.lexicon.domain.Sense
import app.sensee.verification.core.LexicalVerificationQuery
import app.sensee.verification.core.LexicalVerificationReport
import app.sensee.verification.core.LexicalVerifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SuggestVocabularySensesUseCaseTest {
    private class FakeVocabularyRepository : VocabularyRepository {
        var updateReturnsNull = false
        var createDraftCount = 0
            private set
        var updatedDraft: LexicalEntry? = null
            private set

        override suspend fun createDraft(term: String): LexicalEntry {
            createDraftCount++
            return LexicalEntry(EntryId("e1"), term.trim(), EntryStatus.Draft)
        }

        override suspend fun updateDraftTerm(
            id: EntryId,
            term: String,
        ): LexicalEntry? {
            if (updateReturnsNull) return null
            return LexicalEntry(id, term.trim(), EntryStatus.Draft)
                .also { updatedDraft = it }
        }

        override suspend fun getEntry(id: EntryId): LexicalEntry? = null

        override suspend fun listEntries(): List<LexicalEntry> = emptyList()

        override fun observeEntries(): Flow<List<LexicalEntry>> = flowOf(emptyList())

        override suspend fun deleteEntry(id: EntryId) = Unit

        override suspend fun confirmSenses(
            id: EntryId,
            senses: List<Sense>,
        ): LexicalEntry = LexicalEntry(id, "x", EntryStatus.Confirmed, senses)

        override suspend fun updateConfirmedEntry(
            id: EntryId,
            term: String,
            senses: List<Sense>,
        ): LexicalEntry? = LexicalEntry(id, term.trim(), EntryStatus.Confirmed, senses)
    }

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
            suggestions = translations.map { EnrichmentSuggestion(translation = it) },
        )

    @Test
    fun `invoke creates a draft once and reuses an existing draft id`() =
        runTest {
            val repo = FakeVocabularyRepository()
            val useCase =
                SuggestVocabularySensesUseCase(
                    repo,
                    FakeAi(available("наткнуться")),
                    UnavailableVerifier,
                    noTaxonomyProvider,
                    noOpAppDiagnostics(),
                )

            val first = useCase(term = "come across", existingDraftId = null)
            useCase(term = "come acros", existingDraftId = first.draftId)

            assertEquals(1, repo.createDraftCount, "one draft per capture session, no orphan rows")
            assertEquals(EntryId("e1"), first.draftId)
            assertEquals("come acros", repo.updatedDraft?.term)
        }

    @Test
    fun `invoke creates a new draft when the previous draft is no longer editable`() =
        runTest {
            val repo = FakeVocabularyRepository().also { it.updateReturnsNull = true }
            val useCase =
                SuggestVocabularySensesUseCase(
                    repo,
                    FakeAi(available("наткнуться")),
                    UnavailableVerifier,
                    noTaxonomyProvider,
                    noOpAppDiagnostics(),
                )

            val result = useCase(term = "come acros", existingDraftId = EntryId("old"))

            assertEquals(1, repo.createDraftCount)
            assertEquals(EntryId("e1"), result.draftId)
        }

    @Test
    fun `invoke maps enrichment suggestions into candidates`() =
        runTest {
            val useCase =
                SuggestVocabularySensesUseCase(
                    FakeVocabularyRepository(),
                    FakeAi(available("наткнуться", "произвести впечатление")),
                    UnavailableVerifier,
                    noTaxonomyProvider,
                    noOpAppDiagnostics(),
                )

            val result = useCase(term = "come across", existingDraftId = null)

            assertEquals(
                listOf("наткнуться", "произвести впечатление"),
                result.candidates.map { it.translation },
            )
            assertEquals(EnrichmentAvailability.Available, result.availability)
        }

    @Test
    fun `invoke surfaces unavailable enrichment without creating a second draft`() =
        runTest {
            val repo = FakeVocabularyRepository()
            val useCase =
                SuggestVocabularySensesUseCase(
                    repo,
                    FakeAi(EnrichmentResult(EnrichmentAvailability.Unavailable("no key"))),
                    UnavailableVerifier,
                    noTaxonomyProvider,
                    noOpAppDiagnostics(),
                )

            val result = useCase(term = "come across", existingDraftId = EntryId("kept"))

            assertEquals(0, repo.createDraftCount)
            assertEquals(EntryId("kept"), result.draftId)
            assertTrue(result.candidates.isEmpty())
            assertTrue(result.availability is EnrichmentAvailability.Unavailable)
        }

    @Test
    fun `an unavailable verifier means the AI request rides with no evidence attached`() =
        runTest {
            val ai = RecordingAi(available("наткнуться"))
            val useCase =
                SuggestVocabularySensesUseCase(
                    FakeVocabularyRepository(),
                    ai,
                    UnavailableVerifier,
                    noTaxonomyProvider,
                    noOpAppDiagnostics(),
                )

            useCase(term = "come across", existingDraftId = null)

            assertNull(ai.lastRequest?.evidence, "Unavailable verifier must not contribute evidence")
        }

    @Test
    fun `a license-permissive verifier feeds evidence into the AI request`() =
        runTest {
            val ai = RecordingAi(available("наткнуться"))
            val useCase =
                SuggestVocabularySensesUseCase(
                    FakeVocabularyRepository(),
                    ai,
                    CuratedVerifier,
                    noTaxonomyProvider,
                    noOpAppDiagnostics(),
                )

            useCase(term = "come across", existingDraftId = null)

            val evidence = ai.lastRequest?.evidence
            assertEquals("come", evidence?.lemma)
            assertEquals("phrasal_verb", evidence?.entryType)
            assertEquals("come", evidence?.unit?.headLemma)
            assertEquals(listOf("test-fixture"), evidence?.sources?.map { it.sourceId })
        }

    @Test
    fun `language tags are passed to verifier and enrichment request`() =
        runTest {
            val ai = RecordingAi(available("наткнуться"))
            val verifier = RecordingVerifier()
            val useCase =
                SuggestVocabularySensesUseCase(
                    FakeVocabularyRepository(),
                    ai,
                    verifier,
                    noTaxonomyProvider,
                    noOpAppDiagnostics(),
                )

            useCase(
                term = "come across",
                existingDraftId = null,
                studyLanguageTag = "en-GB",
                nativeLanguageTag = "ru-RU",
            )

            assertEquals("en-GB", verifier.lastQuery?.studyLanguageTag)
            assertEquals("en-GB", ai.lastRequest?.studyLanguageTag)
            assertEquals("ru-RU", ai.lastRequest?.nativeLanguageTag)
        }

    @Test
    fun `evidence is dropped when the only source forbids LLM context`() =
        runTest {
            val ai = RecordingAi(available("наткнуться"))
            val useCase =
                SuggestVocabularySensesUseCase(
                    FakeVocabularyRepository(),
                    ai,
                    RestrictedVerifier,
                    noTaxonomyProvider,
                    noOpAppDiagnostics(),
                )

            useCase(term = "come across", existingDraftId = null)

            assertNull(
                ai.lastRequest?.evidence,
                "A source with usableAsLlmContext=false must not leak into the LLM prompt",
            )
        }

    @Test
    fun `source-less family facts are not sent as LLM evidence`() =
        runTest {
            val ai = RecordingAi(available("наткнуться"))
            val useCase =
                SuggestVocabularySensesUseCase(
                    FakeVocabularyRepository(),
                    ai,
                    SourceLessFamilyVerifier,
                    noTaxonomyProvider,
                    noOpAppDiagnostics(),
                )

            useCase(term = "come across", existingDraftId = null)

            val evidence = ai.lastRequest?.evidence
            assertEquals("phrasal_verb", evidence?.entryType)
            assertNull(evidence?.lemma)
            assertNull(evidence?.unit)
            assertNull(evidence?.family)
        }

    @Test
    fun `manual completion uses minimal coverage with verifier evidence`() =
        runTest {
            val ai = RecordingAi(available("дополненный смысл"))
            val useCase =
                SuggestVocabularySensesUseCase(
                    FakeVocabularyRepository(),
                    ai,
                    CuratedVerifier,
                    noTaxonomyProvider,
                    noOpAppDiagnostics(),
                )

            val result =
                useCase.completeManualSense(
                    term = "come across",
                    userNote = "мой черновик",
                )

            assertEquals(listOf("дополненный смысл"), result.map { it.translation })
            assertEquals(SenseCoverage.Minimal, ai.lastRequest?.senseCoverage)
            assertEquals("мой черновик", ai.lastRequest?.userNote)
            assertEquals("phrasal_verb", ai.lastRequest?.evidence?.entryType)
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
