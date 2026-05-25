package app.sensee.feature.vocabularyEditor.domain

import app.sensee.ai.core.AiEnrichmentClient
import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import dev.zacsweers.metro.Inject

/**
 * Capture-suggestion orchestration: ensure a single draft per capture session,
 * enrich the term through the AI seam, map the result into structured
 * candidates. UI copy (availability note, loading state) stays in presentation.
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

    /**
     * [existingDraftId] reuses the session's draft, so re-running suggest
     * (re-entry, an edited term) never spawns an orphan row. [term] is expected
     * already trimmed (input sanitation is a presentation concern).
     */
    public suspend operator fun invoke(
        term: String,
        existingDraftId: EntryId?,
    ): Suggestion {
        // One draft per capture session; edited input updates that draft
        // instead of spawning an orphan row or confirming stale text.
        val draft =
            existingDraftId
                ?.let { vocabularyRepository.updateDraftTerm(it, term) }
                ?: vocabularyRepository.createDraft(term)
        val draftId = draft.id
        // Input language is auto-detected by the seam; only the term is sent.
        val result = enrichmentClient.enrich(EnrichmentRequest(term = term))
        return Suggestion(
            draftId = draftId,
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
