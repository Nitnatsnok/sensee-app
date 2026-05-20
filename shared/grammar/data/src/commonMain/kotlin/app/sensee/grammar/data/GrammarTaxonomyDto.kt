package app.sensee.grammar.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape of `practice/grammar/taxonomy`: the unit-type / category / form
 * tree plus the usage-axis and complement-type label dictionaries. Ids are the
 * neutral wire/storage contract (canon `docs/pos-and-forms.adoc`); `label`
 * fields are learner-facing display text. Forward-compatible: unknown fields
 * are ignored, new sections default to empty.
 */
@Serializable
public data class GrammarTaxonomyDto(
    @SerialName("unit_types") val unitTypes: List<GrammarUnitTypeDto> = emptyList(),
    @SerialName("usage_axes") val usageAxes: List<GrammarUsageAxisDto> = emptyList(),
    @SerialName("complement_types") val complementTypes: List<GrammarLabelDto> = emptyList(),
)

@Serializable
public data class GrammarUnitTypeDto(
    val id: String,
    val label: String,
    val abbreviation: String = "",
    val categories: List<GrammarCategoryDto> = emptyList(),
)

@Serializable
public data class GrammarCategoryDto(
    val id: String,
    val label: String,
    val forms: List<GrammarFormDto> = emptyList(),
)

@Serializable
public data class GrammarFormDto(
    val id: String,
    val label: String,
    val abbreviations: List<String> = emptyList(),
    val examples: List<String> = emptyList(),
)

@Serializable
public data class GrammarUsageAxisDto(
    val id: String,
    val label: String,
    val values: List<GrammarLabelDto> = emptyList(),
)

@Serializable
public data class GrammarLabelDto(
    val id: String,
    val label: String,
)
