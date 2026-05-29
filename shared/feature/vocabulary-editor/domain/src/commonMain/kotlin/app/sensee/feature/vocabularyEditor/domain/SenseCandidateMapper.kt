package app.sensee.feature.vocabularyEditor.domain

import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.core.EnrichmentSuggestion
import app.sensee.grammar.domain.TaxonomyInvariants
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.enrichment.EnrichedSense
import app.sensee.lexicon.enrichment.toEnrichedSense
import app.sensee.lexicon.enrichment.toEnrichedSenses

/**
 * Vocabulary Editor candidate boundary: the shared lexicon mapper turns AI
 * enrichment into [Sense], while this feature adds selection identity and
 * verification metadata for the capture/edit workflow.
 */
public fun EnrichmentSuggestion.toSenseCandidate(
    fallbackTerm: String,
    invariants: TaxonomyInvariants? = null,
    onUnknown: (field: String, id: String) -> Unit = { _, _ -> },
): SenseCandidate = toEnrichedSense(fallbackTerm, invariants, onUnknown).toSenseCandidate()

public fun EnrichmentResult.toSenseCandidates(
    fallbackTerm: String,
    invariants: TaxonomyInvariants? = null,
    onUnknown: (field: String, id: String) -> Unit = { _, _ -> },
): List<SenseCandidate> =
    toEnrichedSenses(fallbackTerm, invariants, onUnknown)
        .map(EnrichedSense::toSenseCandidate)
        .withUniqueIds()

public fun EnrichmentSuggestion.toMeaningCandidate(
    fallbackTerm: String,
    invariants: TaxonomyInvariants? = null,
    onUnknown: (field: String, id: String) -> Unit = { _, _ -> },
): MeaningCandidate =
    toSenseCandidate(
        fallbackTerm = fallbackTerm,
        invariants = invariants,
        onUnknown = onUnknown,
    )

public fun EnrichmentResult.toMeaningCandidates(
    fallbackTerm: String,
    invariants: TaxonomyInvariants? = null,
    onUnknown: (field: String, id: String) -> Unit = { _, _ -> },
): List<MeaningCandidate> =
    toSenseCandidates(
        fallbackTerm = fallbackTerm,
        invariants = invariants,
        onUnknown = onUnknown,
    )

private fun EnrichedSense.toSenseCandidate(): SenseCandidate =
    SenseCandidate(
        id =
            deriveSenseCandidateId(
                translation = sense.translation,
                surfaceForm = sense.surfaceForm,
                unitType = sense.unitType,
                contentFingerprint = contentFingerprint,
            ),
        sense = sense,
    )

private fun List<SenseCandidate>.withUniqueIds(): List<SenseCandidate> {
    val occurrencesById = mutableMapOf<SenseCandidateId, Int>()
    return map { candidate ->
        val occurrence = (occurrencesById[candidate.id] ?: 0) + 1
        occurrencesById[candidate.id] = occurrence
        if (occurrence == 1) {
            candidate
        } else {
            candidate.copy(id = SenseCandidateId("${candidate.id.value}#$occurrence"))
        }
    }
}
