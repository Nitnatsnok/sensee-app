package app.sensee.grammar.data

import app.sensee.grammar.domain.TaxonomyInvariants

/**
 * Builds [TaxonomyInvariants] from a fetched [GrammarTaxonomyDto]. The forms
 * tree is flattened into a per-category union — a category id can appear
 * under several unit types (`verb` under `verb` and `phrasal_verb`), and
 * `(category, form)` pairs are validated independently of the unit type.
 */
public fun GrammarTaxonomyDto.toTaxonomyInvariants(): TaxonomyInvariants {
    val formsByCategory = mutableMapOf<String, MutableSet<String>>()
    unitTypes.forEach { unit ->
        unit.categories.forEach { category ->
            val bucket = formsByCategory.getOrPut(category.id) { mutableSetOf() }
            category.forms.forEach { form -> bucket += form.id }
        }
    }
    return TaxonomyInvariants(
        knownUnitTypeIds = unitTypes.map { it.id }.toSet(),
        knownComplementIds = complementTypes.map { it.id }.toSet(),
        allowedValuesByAxis =
            usageAxes.associate { axis ->
                axis.id to axis.values.map { it.id }.toSet()
            },
        allowedFormsByCategory = formsByCategory.mapValues { it.value.toSet() },
    )
}
