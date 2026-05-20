package app.sensee.feature.vocabularyEditor.domain

import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.core.EnrichmentSuggestion
import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.PrepositionGovernment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.grammar.domain.UsageLabel

/**
 * Anti-corruption mapping at the feature boundary (ADR-005): the AI seam emits
 * neutral string-based [EnrichmentSuggestion]s; this feature owns turning them
 * into structured [MeaningCandidate]s. A blank surface form falls back to the
 * captured term. An unknown unit type or a grammar pair that violates the
 * category -> form invariant is dropped, never forced into the model.
 */
public fun EnrichmentSuggestion.toMeaningCandidate(fallbackTerm: String): MeaningCandidate {
    val surfaceText = surfaceForm?.takeIf { it.isNotBlank() } ?: fallbackTerm
    return MeaningCandidate(
        translation = translation,
        surfaceForm = SurfaceForm.parse(surfaceText),
        unitType = unitType?.let { GrammarUnitType.fromId(it) },
        baseLemma = baseLemma?.takeIf { it.isNotBlank() },
        explanation = explanation,
        contextualApplications = examples.map { ContextualApplication(StudiedSentence.parse(it)) },
        governedPrepositions =
            governedPrepositions.map { PrepositionGovernment(it.alternatives, it.example) },
        complementation = complementation.mapNotNull { ComplementType.fromId(it) },
        usageLabels = usageLabels.mapNotNull { UsageLabel.resolve(it.axis, it.value) },
        usageNote = usageNote,
        grammarTags = grammarTags.mapNotNull { GrammarTag.resolve(it.category, it.form) },
        irregularForms =
            irregularForms?.let { IrregularForms(it.base, it.past, it.pastParticiple) },
    )
}

public fun EnrichmentResult.toMeaningCandidates(fallbackTerm: String): List<MeaningCandidate> =
    suggestions.map { it.toMeaningCandidate(fallbackTerm) }
