package app.sensee.feature.vocabularyEditor.domain

import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.core.EnrichmentSuggestion
import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.PrepositionGovernment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.grammar.domain.TaxonomyInvariants
import app.sensee.grammar.domain.UsageAxis
import app.sensee.grammar.domain.UsageLabel
import app.sensee.grammar.domain.UsageValue

/**
 * Anti-corruption mapping at the feature boundary (ADR-005): turns the
 * AI-seam's neutral [EnrichmentSuggestion] into a structured [MeaningCandidate].
 *
 * [invariants] controls per-field validation:
 * - `null` ⇒ pass-through; unknown ids surface as `Unknown(id)`.
 * - non-null ⇒ strict; ids absent from the loaded taxonomy are dropped,
 *   ids the client does not know still pass as `Unknown(id)`.
 *
 * [onUnknown] is invoked once per resolved `Unknown` branch; use
 * [warnUnknownTaxonomyValue] as the default sink.
 */
public fun EnrichmentSuggestion.toMeaningCandidate(
    fallbackTerm: String,
    invariants: TaxonomyInvariants? = null,
    onUnknown: (field: String, id: String) -> Unit = { _, _ -> },
): MeaningCandidate {
    val surfaceText = surfaceForm?.takeIf { it.isNotBlank() } ?: fallbackTerm
    val parsedSurfaceForm = SurfaceForm.parse(surfaceText)
    val resolvedUnitType = resolveUnitType(unitType, invariants, onUnknown)
    return MeaningCandidate(
        id =
            deriveMeaningCandidateId(
                translation = translation,
                surfaceForm = parsedSurfaceForm,
                unitType = resolvedUnitType,
                contentFingerprint = identityFingerprint(),
            ),
        translation = translation,
        surfaceForm = parsedSurfaceForm,
        unitType = resolvedUnitType,
        baseLemma = baseLemma?.takeIf { it.isNotBlank() },
        explanation = explanation,
        contextualApplications = examples.map { ContextualApplication(StudiedSentence.parse(it)) },
        governedPrepositions =
            governedPrepositions.map { PrepositionGovernment(it.alternatives, it.example) },
        complementation = complementation.mapNotNull { resolveComplement(it, invariants, onUnknown) },
        usageLabels = usageLabels.mapNotNull { resolveUsageLabel(it.axis, it.value, invariants, onUnknown) },
        usageNote = usageNote,
        grammarTags = grammarTags.mapNotNull { resolveGrammarTag(it.category, it.form, invariants, onUnknown) },
        irregularForms =
            irregularForms?.let { IrregularForms(it.base, it.past, it.pastParticiple) },
    )
}

public fun EnrichmentResult.toMeaningCandidates(
    fallbackTerm: String,
    invariants: TaxonomyInvariants? = null,
    onUnknown: (field: String, id: String) -> Unit = { _, _ -> },
): List<MeaningCandidate> =
    suggestions
        .map { it.toMeaningCandidate(fallbackTerm, invariants, onUnknown) }
        .withUniqueIds()

// Field joiner: ASCII Unit Separator (U+001F); between-field joiner: Record
// Separator (U+001E). Both are control characters and cannot appear in any
// user-visible string, so they're safe to use as collision-free delimiters.
private fun EnrichmentSuggestion.identityFingerprint(): String =
    listOf(
        baseLemma.orEmpty(),
        explanation.orEmpty(),
        examples.joinToString(separator = "\u001f"),
        governedPrepositions.joinToString(separator = "\u001f") {
            "${it.alternatives.joinToString(separator = ",")}:${it.example.orEmpty()}"
        },
        complementation.joinToString(separator = "\u001f"),
        usageLabels.joinToString(separator = "\u001f") { "${it.axis}:${it.value}" },
        usageNote.orEmpty(),
        grammarTags.joinToString(separator = "\u001f") { "${it.category}:${it.form}" },
        irregularForms
            ?.let { "${it.base}:${it.past}:${it.pastParticiple}" }
            .orEmpty(),
    ).joinToString(separator = "\u001e")

private fun List<MeaningCandidate>.withUniqueIds(): List<MeaningCandidate> {
    val occurrencesById = mutableMapOf<MeaningCandidateId, Int>()
    return map { candidate ->
        val occurrence = (occurrencesById[candidate.id] ?: 0) + 1
        occurrencesById[candidate.id] = occurrence
        if (occurrence == 1) {
            candidate
        } else {
            candidate.copy(id = MeaningCandidateId("${candidate.id.value}#$occurrence"))
        }
    }
}

private fun resolveUnitType(
    id: String?,
    invariants: TaxonomyInvariants?,
    onUnknown: (String, String) -> Unit,
): GrammarUnitType? {
    if (id.isNullOrBlank()) return null
    val resolved = GrammarUnitType.fromId(id)
    if (invariants != null && resolved.id !in invariants.knownUnitTypeIds) return null
    if (resolved is GrammarUnitType.Unknown) onUnknown("unit_type", resolved.id)
    return resolved
}

private fun resolveComplement(
    id: String,
    invariants: TaxonomyInvariants?,
    onUnknown: (String, String) -> Unit,
): ComplementType? {
    val resolved = ComplementType.fromId(id)
    if (invariants != null && resolved.id !in invariants.knownComplementIds) return null
    if (resolved is ComplementType.Unknown) onUnknown("complementation", resolved.id)
    return resolved
}

private fun resolveUsageLabel(
    axisId: String,
    valueId: String,
    invariants: TaxonomyInvariants?,
    onUnknown: (String, String) -> Unit,
): UsageLabel? {
    val label =
        UsageLabel.resolve(axisId, valueId, invariants?.allowedValuesByAxis) ?: return null
    if (label.axis is UsageAxis.Unknown) onUnknown("usage_label_axis", label.axis.id)
    if (label.value is UsageValue.Unknown) onUnknown("usage_label_value", label.value.id)
    return label
}

private fun resolveGrammarTag(
    categoryId: String,
    formId: String,
    invariants: TaxonomyInvariants?,
    onUnknown: (String, String) -> Unit,
): GrammarTag? {
    val tag =
        GrammarTag.resolve(categoryId, formId, invariants?.allowedFormsByCategory) ?: return null
    if (tag.category is GrammarCategory.Unknown) onUnknown("grammar_tag_category", tag.category.id)
    if (tag.form is GrammarForm.Unknown) onUnknown("grammar_tag_form", tag.form.id)
    return tag
}
