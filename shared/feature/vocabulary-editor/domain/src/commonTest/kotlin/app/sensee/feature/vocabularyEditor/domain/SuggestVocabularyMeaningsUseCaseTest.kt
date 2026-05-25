package app.sensee.feature.vocabularyEditor.domain

import app.sensee.ai.core.AiEnrichmentClient
import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.core.EnrichmentSuggestion
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SuggestVocabularyMeaningsUseCaseTest {
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

        override suspend fun confirmMeanings(
            id: EntryId,
            meanings: List<Meaning>,
        ): LexicalEntry = LexicalEntry(id, "x", EntryStatus.Confirmed, meanings)
    }

    private class FakeAi(
        private val result: EnrichmentResult,
    ) : AiEnrichmentClient {
        override suspend fun enrich(request: EnrichmentRequest): EnrichmentResult = result
    }

    private val noTaxonomyProvider = TaxonomyInvariantsProvider { null }

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
                SuggestVocabularyMeaningsUseCase(
                    repo,
                    FakeAi(available("наткнуться")),
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
                SuggestVocabularyMeaningsUseCase(
                    repo,
                    FakeAi(available("наткнуться")),
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
                SuggestVocabularyMeaningsUseCase(
                    FakeVocabularyRepository(),
                    FakeAi(available("наткнуться", "произвести впечатление")),
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
                SuggestVocabularyMeaningsUseCase(
                    repo,
                    FakeAi(EnrichmentResult(EnrichmentAvailability.Unavailable("no key"))),
                    noTaxonomyProvider,
                    noOpAppDiagnostics(),
                )

            val result = useCase(term = "come across", existingDraftId = EntryId("kept"))

            assertEquals(0, repo.createDraftCount)
            assertEquals(EntryId("kept"), result.draftId)
            assertTrue(result.candidates.isEmpty())
            assertTrue(result.availability is EnrichmentAvailability.Unavailable)
        }
}
