package app.sensee.ai.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Versioned wire DTO at the provider boundary. Providers (or fixtures) produce
 * this shape; it never leaks past [EnrichmentResponseMapper] into features. The
 * version field is forward-design (ADR-005), not a migration history: one
 * uniform sense model for word/phrase/idiom and for either input language.
 */
@Serializable
public data class EnrichmentResponseV1(
    @SerialName("version") val version: Int = SCHEMA_VERSION,
    @SerialName("items") val items: List<EnrichmentItemV1> = emptyList(),
) {
    public companion object {
        public const val SCHEMA_VERSION: Int = 1
    }
}

@Serializable
public data class EnrichmentItemV1(
    @SerialName("translation") val translation: String? = null,
    @SerialName("surface_form") val surfaceForm: String? = null,
    @SerialName("unit_type") val unitType: String? = null,
    @SerialName("base_lemma") val baseLemma: String? = null,
    @SerialName("explanation") val explanation: String? = null,
    @SerialName("examples") val examples: List<String> = emptyList(),
    @SerialName("preposition_government")
    val prepositionGovernment: List<PrepositionGovernmentDtoV1> = emptyList(),
    @SerialName("complementation") val complementation: List<String> = emptyList(),
    @SerialName("usage_labels") val usageLabels: List<UsageLabelDtoV1> = emptyList(),
    @SerialName("usage_note") val usageNote: String? = null,
    @SerialName("grammar_tags") val grammarTags: List<GrammarTagDtoV1> = emptyList(),
    @SerialName("irregular_forms") val irregularForms: IrregularFormsDtoV1? = null,
)

@Serializable
public data class PrepositionGovernmentDtoV1(
    @SerialName("alternatives") val alternatives: List<String> = emptyList(),
    @SerialName("example") val example: String? = null,
)

@Serializable
public data class GrammarTagDtoV1(
    @SerialName("category") val category: String,
    @SerialName("form") val form: String,
)

@Serializable
public data class UsageLabelDtoV1(
    @SerialName("axis") val axis: String,
    @SerialName("value") val value: String,
)

@Serializable
public data class IrregularFormsDtoV1(
    @SerialName("base") val base: String,
    @SerialName("past") val past: String,
    @SerialName("past_participle") val pastParticiple: String,
)
