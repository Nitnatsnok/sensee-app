package app.sensee.grammar.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape of `practice/grammar/taxonomy`: the unit-type / category / form
 * tree plus the usage-axis and complement-type label dictionaries. Ids are the
 * neutral wire/storage contract (canon `docs/domain/pos-and-forms.adoc`); the `labels`
 * map carries one entry per supported UI language (BCP-47 tag → long/short
 * label pair). Forward-compatible: unknown fields are ignored, new sections
 * default to empty, missing languages fall back to `null` at lookup time.
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
    val labels: Map<String, GrammarLabelTranslationDto> = emptyMap(),
    val categories: List<GrammarCategoryDto> = emptyList(),
)

@Serializable
public data class GrammarCategoryDto(
    val id: String,
    val labels: Map<String, GrammarLabelTranslationDto> = emptyMap(),
    val forms: List<GrammarFormDto> = emptyList(),
)

@Serializable
public data class GrammarFormDto(
    val id: String,
    val labels: Map<String, GrammarLabelTranslationDto> = emptyMap(),
    val examples: List<String> = emptyList(),
)

@Serializable
public data class GrammarUsageAxisDto(
    val id: String,
    val labels: Map<String, GrammarLabelTranslationDto> = emptyMap(),
    val values: List<GrammarLabelDto> = emptyList(),
)

@Serializable
public data class GrammarLabelDto(
    val id: String,
    val labels: Map<String, GrammarLabelTranslationDto> = emptyMap(),
)

/**
 * One language's display form for a grammar id: the descriptive [long] form
 * plus a map of stylistic short variants keyed by style id
 * (`lexicographic` / `pedagogical`, see `GrammarLabelStyle` in the domain).
 * A missing style falls back to [long] at resolution time; a missing entry
 * for the requested language resolves to `null`.
 */
@Serializable
public data class GrammarLabelTranslationDto(
    val long: String,
    val short: Map<String, String> = emptyMap(),
)
