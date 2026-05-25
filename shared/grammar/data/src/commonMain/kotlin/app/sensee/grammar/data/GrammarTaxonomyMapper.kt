package app.sensee.grammar.data

import app.sensee.grammar.domain.GrammarLabel
import app.sensee.grammar.domain.GrammarLabelStyle
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.normalizedTaxonomyId

/**
 * Builds the pure [GrammarLabels] resolver from a fetched taxonomy. The DTO
 * (wire shape) is owned here; the resolver type is neutral and lives in
 * `shared/grammar/domain` so lightweight consumers (presentation APIs) need
 * only the domain, not this data module. Each id carries a per-language
 * `{long, short: { style -> abbr }}` shape; the resolver lookup picks the
 * right one by language tag + form + style.
 *
 * The mapper also keeps a per-form (no-category) index in
 * [GrammarLabels.formByName] so the practice help glossary can label a flat
 * list of forms without having to reconstruct a tag.
 */
public fun GrammarTaxonomyDto.toGrammarLabels(): GrammarLabels {
    val unitTypes = mutableMapOf<String, Map<String, GrammarLabel>>()
    val categories = mutableMapOf<String, Map<String, GrammarLabel>>()
    val forms = mutableMapOf<String, Map<String, GrammarLabel>>()
    val formsByName = mutableMapOf<String, Map<String, GrammarLabel>>()
    this.unitTypes.forEach { unit ->
        addUnitTypeLabels(
            unit = unit,
            unitTypes = unitTypes,
            categories = categories,
            forms = forms,
            formsByName = formsByName,
        )
    }
    val usageValues = mutableMapOf<String, Map<String, GrammarLabel>>()
    usageAxes.forEach { axis ->
        axis.values.forEach { value ->
            usageValues[GrammarLabels.key(axis.id, value.id)] = value.labels.toDomainLabels()
        }
    }
    return GrammarLabels(
        unitTypeLabels = unitTypes,
        categoryLabels = categories,
        formLabels = forms,
        formLabelsByName = formsByName,
        usageValueLabels = usageValues,
        complementLabels = complementTypes.associate { it.id to it.labels.toDomainLabels() },
    )
}

private fun addUnitTypeLabels(
    unit: GrammarUnitTypeDto,
    unitTypes: MutableMap<String, Map<String, GrammarLabel>>,
    categories: MutableMap<String, Map<String, GrammarLabel>>,
    forms: MutableMap<String, Map<String, GrammarLabel>>,
    formsByName: MutableMap<String, Map<String, GrammarLabel>>,
) {
    unitTypes[unit.id.normalizedTaxonomyId()] = unit.labels.toDomainLabels()
    unit.categories.forEach { category ->
        addCategoryLabels(
            category = category,
            categories = categories,
            forms = forms,
            formsByName = formsByName,
        )
    }
}

private fun addCategoryLabels(
    category: GrammarCategoryDto,
    categories: MutableMap<String, Map<String, GrammarLabel>>,
    forms: MutableMap<String, Map<String, GrammarLabel>>,
    formsByName: MutableMap<String, Map<String, GrammarLabel>>,
) {
    categories[category.id] = category.labels.toDomainLabels()
    // The fixture is the taxonomy source of truth here — every (category, form)
    // pair it carries is by definition allowed. No need to round-trip through
    // GrammarTag.resolve, which without an allowed-pairs map is a no-op.
    category.forms.forEach { form ->
        val domainLabels = form.labels.toDomainLabels()
        forms[GrammarLabels.key(category.id, form.id)] = domainLabels
        // First occurrence wins — the per-form abbreviation is consistent
        // across the categories that admit the form (verb/verb_irregular
        // share `infinitive` etc.), so any one of them is canonical.
        if (form.id !in formsByName) formsByName[form.id] = domainLabels
    }
}

private fun Map<String, GrammarLabelTranslationDto>.toDomainLabels(): Map<String, GrammarLabel> =
    mapValues { (_, translation) ->
        GrammarLabel(
            long = translation.long,
            short = translation.short.toDomainShortStyles(),
        )
    }

private fun Map<String, String>.toDomainShortStyles(): Map<GrammarLabelStyle, String> =
    mapNotNull { (styleId, abbreviation) ->
        val style = STYLE_BY_ID[styleId] ?: return@mapNotNull null
        style to abbreviation
    }.toMap()

private val STYLE_BY_ID: Map<String, GrammarLabelStyle> =
    GrammarLabelStyle.entries.associateBy { it.name.lowercase() }
