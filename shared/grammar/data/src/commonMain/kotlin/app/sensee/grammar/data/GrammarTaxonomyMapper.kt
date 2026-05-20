package app.sensee.grammar.data

import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarTag

/**
 * Builds the pure [GrammarLabels] resolver from a fetched taxonomy. The DTO
 * (wire shape) is owned here; the resolver type is neutral and lives in
 * `shared/grammar/domain` so lightweight consumers (presentation APIs) need
 * only the domain, not this data module.
 */
public fun GrammarTaxonomyDto.toGrammarLabels(): GrammarLabels {
    val unitTypes = mutableMapOf<String, String>()
    val categories = mutableMapOf<String, String>()
    val forms = mutableMapOf<String, String>()
    this.unitTypes.forEach { unit ->
        addUnitTypeLabels(
            unit = unit,
            unitTypes = unitTypes,
            categories = categories,
            forms = forms,
        )
    }
    val usageValues = mutableMapOf<String, String>()
    usageAxes.forEach { axis ->
        axis.values.forEach { value ->
            usageValues[GrammarLabels.key(axis.id, value.id)] = value.label
        }
    }
    return GrammarLabels(
        unitTypeLabels = unitTypes,
        categoryLabels = categories,
        formLabels = forms,
        usageValueLabels = usageValues,
        complementLabels = complementTypes.associate { it.id to it.label },
    )
}

private fun addUnitTypeLabels(
    unit: GrammarUnitTypeDto,
    unitTypes: MutableMap<String, String>,
    categories: MutableMap<String, String>,
    forms: MutableMap<String, String>,
) {
    unitTypes[unit.id.normalizedId()] = unit.label
    unit.categories.forEach { category ->
        addCategoryLabels(
            category = category,
            categories = categories,
            forms = forms,
        )
    }
}

private fun addCategoryLabels(
    category: GrammarCategoryDto,
    categories: MutableMap<String, String>,
    forms: MutableMap<String, String>,
) {
    categories[category.id] = category.label
    category.forms.forEach { form ->
        if (GrammarTag.resolve(category.id, form.id) != null) {
            forms[GrammarLabels.key(category.id, form.id)] = form.label
        }
    }
}

private fun String.normalizedId(): String = filter { it.isLetterOrDigit() }.lowercase()
