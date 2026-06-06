package app.sensee.feature.vocabularyEditor.domain.mapping

import app.sensee.ai.core.contract.EnrichmentResult
import app.sensee.ai.core.model.EnrichmentExample
import app.sensee.ai.core.model.EnrichmentSuggestion
import app.sensee.ai.core.model.IrregularFormsHint
import app.sensee.ai.core.model.UnitComponentHint
import app.sensee.ai.core.model.WordFamilyHint
import app.sensee.ai.core.model.cefrLevelId
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
import app.sensee.lexicon.domain.CefrLevel
import app.sensee.lexicon.domain.ContextualApplication
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.WordFamilyMember

/**
 * Maps an AI enrichment suggestion into the central lexical [Sense] shape. This
 * is the anti-corruption boundary between the AI seam and the canon: a suggestion
 * becomes a [Sense] directly (ADR-001), there is no intermediate wrapper. It lives
 * in Vocabulary Editor — capture is the only producer that maps AI output into the
 * canon; the catalog path uses `SenseDto.toDomain()` instead.
 *
 * [invariants] controls per-field validation:
 * - `null` means pass-through; unknown ids surface as `Unknown(id)`.
 * - non-null means strict; ids absent from the loaded taxonomy are dropped.
 */
public fun EnrichmentSuggestion.toSense(
    fallbackTerm: String,
    invariants: TaxonomyInvariants? = null,
    onUnknown: (field: String, id: String) -> Unit = { _, _ -> },
): Sense {
    val surfaceText = surfaceForm?.takeIf { it.isNotBlank() } ?: fallbackTerm
    val parsedSurfaceForm = SurfaceForm.parse(surfaceText)
    val resolvedUnitType =
        resolveUnitType(
            id = unitType,
            irregularForms = irregularForms,
            invariants = invariants,
            onUnknown = onUnknown,
        )
    return Sense(
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
        cefr = CefrLevel.fromId(cefrLevelId()),
    )
}

public fun EnrichmentResult.toSenses(
    fallbackTerm: String,
    invariants: TaxonomyInvariants? = null,
    onUnknown: (field: String, id: String) -> Unit = { _, _ -> },
): List<Sense> = suggestions.map { it.toSense(fallbackTerm, invariants, onUnknown) }

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
