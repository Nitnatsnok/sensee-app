package app.sensee.feature.vocabularyEditor.domain

import app.sensee.ai.core.AiEnrichmentClient
import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import dev.zacsweers.metro.Inject

/**
 * Source-compatible facade for the unchanged capture presentation API. New
 * domain/data code should use [SuggestVocabularySensesUseCase].
 */
@Inject
public class SuggestVocabularyMeaningsUseCase(
    private val vocabularyRepository: VocabularyRepository,
    private val enrichmentClient: AiEnrichmentClient,
    private val taxonomyInvariantsProvider: TaxonomyInvariantsProvider,
    private val appDiagnostics: AppDiagnostics,
) {
    public data class Suggestion(
        val draftId: EntryId,
        val candidates: List<MeaningCandidate>,
        val availability: EnrichmentAvailability,
    )

    public suspend operator fun invoke(
        term: String,
        existingDraftId: EntryId?,
    ): Suggestion {
        val draft =
            existingDraftId
                ?.let { vocabularyRepository.updateDraftTerm(it, term) }
                ?: vocabularyRepository.createDraft(term)
        val result = enrichmentClient.enrich(EnrichmentRequest(term = term))
        return Suggestion(
            draftId = draft.id,
            candidates =
                result.toMeaningCandidates(
                    fallbackTerm = term,
                    invariants = taxonomyInvariantsProvider.invariants(),
                    onUnknown = appDiagnostics::warnUnknownTaxonomyValue,
                ),
            availability = result.availability,
        )
    }
}
