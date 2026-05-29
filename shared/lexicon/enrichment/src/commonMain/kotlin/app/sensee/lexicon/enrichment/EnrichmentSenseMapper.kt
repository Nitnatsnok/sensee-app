package app.sensee.lexicon.enrichment

import app.sensee.ai.core.EnrichmentExample
import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.core.EnrichmentSuggestion
import app.sensee.ai.core.IrregularFormsHint
import app.sensee.ai.core.UnitComponentHint
import app.sensee.ai.core.WordFamilyHint
import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.ComponentRole
import app.sensee.grammar.domain.ComponentSalience
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.PrepositionGovernment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.grammar.domain.TaxonomyInvariants
import app.sensee.grammar.domain.UnitComponent
import app.sensee.grammar.domain.UsageAxis
import app.sensee.grammar.domain.UsageLabel
import app.sensee.grammar.domain.UsageValue
import app.sensee.lexicon.domain.AlignmentChunk
import app.sensee.lexicon.domain.ContextualApplication
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.WordFamilyMember

/**
 * Maps an AI enrichment suggestion into the central lexical [Sense] shape.
 *
 * [invariants] controls per-field validation:
 * - `null` means pass-through; unknown ids surface as `Unknown(id)`.
 * - non-null means strict; ids absent from the loaded taxonomy are dropped.
 */
public fun EnrichmentSuggestion.toEnrichedSense(
    fallbackTerm: String,
    invariants: TaxonomyInvariants? = null,
    onUnknown: (field: String, id: String) -> Unit = { _, _ -> },
): EnrichedSense {
    val surfaceText = surfaceForm?.takeIf { it.isNotBlank() } ?: fallbackTerm
    val parsedSurfaceForm = SurfaceForm.parse(surfaceText)
    val resolvedUnitType =
        resolveUnitType(
            id = unitType,
            irregularForms = irregularForms,
            invariants = invariants,
            onUnknown = onUnknown,
        )
    return EnrichedSense(
        sense =
            Sense(
                translation = translation,
                surfaceForm = parsedSurfaceForm,
                unitType = resolvedUnitType,
                baseLemma = baseLemma?.takeIf { it.isNotBlank() },
                headLemma = headLemma?.takeIf { it.isNotBlank() },
                components = components.map { resolveComponent(it, onUnknown) },
                explanation = explanation,
                contextualApplications = examples.map(::toContextualApplication),
                governedPrepositions =
                    governedPrepositions.map { PrepositionGovernment(it.alternatives, it.example) },
                complementation = complementation.mapNotNull { resolveComplement(it, invariants, onUnknown) },
                usageLabels = usageLabels.mapNotNull { resolveUsageLabel(it.axis, it.value, invariants, onUnknown) },
                usageNote = usageNote,
                grammarTags = grammarTags.mapNotNull { resolveGrammarTag(it.category, it.form, invariants, onUnknown) },
                irregularForms =
                    irregularForms?.let { IrregularForms(it.base, it.past, it.pastParticiple) },
                synonyms = synonyms,
                antonyms = antonyms,
                collocations = collocations,
                wordFamily = wordFamily.map { resolveWordFamilyMember(it, onUnknown) },
            ),
        contentFingerprint = identityFingerprint(),
    )
}

public fun EnrichmentResult.toEnrichedSenses(
    fallbackTerm: String,
    invariants: TaxonomyInvariants? = null,
    onUnknown: (field: String, id: String) -> Unit = { _, _ -> },
): List<EnrichedSense> = suggestions.map { it.toEnrichedSense(fallbackTerm, invariants, onUnknown) }

public fun EnrichmentSuggestion.toSense(
    fallbackTerm: String,
    invariants: TaxonomyInvariants? = null,
    onUnknown: (field: String, id: String) -> Unit = { _, _ -> },
): Sense = toEnrichedSense(fallbackTerm, invariants, onUnknown).sense

private fun EnrichmentSuggestion.identityFingerprint(): String =
    listOf(
        baseLemma.orEmpty(),
        headLemma.orEmpty(),
        components.joinToString(separator = IDENTITY_FIELD_SEPARATOR) { "${it.text}:${it.role}" },
        explanation.orEmpty(),
        examples.joinToString(separator = IDENTITY_FIELD_SEPARATOR) { example ->
            val alignmentKey = example.alignment.joinToString(separator = ",") { "${it.source}>${it.target}" }
            "${example.sentence}|${example.translation.orEmpty()}|$alignmentKey"
        },
        governedPrepositions.joinToString(separator = IDENTITY_FIELD_SEPARATOR) {
            "${it.alternatives.joinToString(separator = ",")}:${it.example.orEmpty()}"
        },
        complementation.joinToString(separator = IDENTITY_FIELD_SEPARATOR),
        usageLabels.joinToString(separator = IDENTITY_FIELD_SEPARATOR) { "${it.axis}:${it.value}" },
        usageNote.orEmpty(),
        grammarTags.joinToString(separator = IDENTITY_FIELD_SEPARATOR) { "${it.category}:${it.form}" },
        irregularForms
            ?.let { "${it.base}:${it.past}:${it.pastParticiple}" }
            .orEmpty(),
    ).joinToString(separator = IDENTITY_RECORD_SEPARATOR)

private const val IDENTITY_FIELD_SEPARATOR: String = "\u001f"
private const val IDENTITY_RECORD_SEPARATOR: String = "\u001e"

private fun toContextualApplication(example: EnrichmentExample): ContextualApplication =
    ContextualApplication(
        sentence = StudiedSentence.parse(example.sentence),
        translation = example.translation,
        alignment = example.alignment.map { AlignmentChunk(source = it.source, target = it.target) },
    )

private fun resolveUnitType(
    id: String?,
    irregularForms: IrregularFormsHint?,
    invariants: TaxonomyInvariants?,
    onUnknown: (String, String) -> Unit,
): GrammarUnitType? {
    val resolved =
        if (id.isNullOrBlank()) {
            if (irregularForms != null) GrammarUnitType.IrregularVerb else return null
        } else {
            GrammarUnitType.fromId(id)
        }
    val effective =
        if (resolved == GrammarUnitType.Verb && irregularForms != null) {
            GrammarUnitType.IrregularVerb
        } else {
            resolved
        }
    if (invariants != null && effective.id !in invariants.knownUnitTypeIds) return null
    if (effective is GrammarUnitType.Unknown) onUnknown("unit_type", effective.id)
    return effective
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

private fun resolveComponent(
    hint: UnitComponentHint,
    onUnknown: (String, String) -> Unit,
): UnitComponent {
    val role = ComponentRole.fromId(hint.role)
    if (role is ComponentRole.Unknown) onUnknown("component_role", role.id)
    val salience = hint.salience?.takeIf { it.isNotBlank() }?.let { ComponentSalience.fromId(it) }
    if (salience is ComponentSalience.Unknown) onUnknown("component_salience", salience.id)
    return UnitComponent(text = hint.text, role = role, salience = salience)
}

private fun resolveWordFamilyMember(
    hint: WordFamilyHint,
    onUnknown: (String, String) -> Unit,
): WordFamilyMember {
    val unitType = GrammarUnitType.fromId(hint.unitType)
    if (unitType is GrammarUnitType.Unknown) onUnknown("word_family_unit_type", unitType.id)
    return WordFamilyMember(lemma = hint.lemma, unitType = unitType)
}
