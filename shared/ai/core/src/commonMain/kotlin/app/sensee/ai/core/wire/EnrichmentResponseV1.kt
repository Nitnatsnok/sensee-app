package app.sensee.ai.core.wire

import app.sensee.ai.core.contract.EnrichmentResult
import app.sensee.ai.core.model.EnrichmentExample
import app.sensee.ai.core.model.EnrichmentSuggestion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Versioned wire DTO at the provider boundary. Providers produce this shape;
 * it never leaks past [EnrichmentResponseMapper] into features. The active
 * development wire contract currently stays on schema version 1.
 *
 * Intentional boundary mirror (ADR-005/0006): this `*V1` wire family deliberately
 * duplicates the shape of the neutral [EnrichmentSuggestion]/[EnrichmentResult].
 * Do NOT dedup the two — the version skew between wire and neutral model is the
 * whole point of the seam; [EnrichmentResponseMapper] is the single crossing.
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

/** One enriched item on the wire; intentional boundary mirror of [EnrichmentSuggestion] (see [EnrichmentResponseV1]). */
@Serializable
public data class EnrichmentItemV1(
    @SerialName("translation") val translation: String? = null,
    @SerialName("surface_form") val surfaceForm: String? = null,
    @SerialName("unit_type") val unitType: String? = null,
    @SerialName("base_lemma") val baseLemma: String? = null,
    @SerialName("head_lemma") val headLemma: String? = null,
    @SerialName("components") val components: List<UnitComponentDtoV1> = emptyList(),
    @SerialName("explanation") val explanation: String? = null,
    @SerialName("examples") val examples: List<EnrichmentExampleV1> = emptyList(),
    @SerialName("synonyms") val synonyms: List<String> = emptyList(),
    @SerialName("antonyms") val antonyms: List<String> = emptyList(),
    @SerialName("collocations") val collocations: List<String> = emptyList(),
    @SerialName("word_family") val wordFamily: List<WordFamilyEntryDtoV1> = emptyList(),
    @SerialName("preposition_government")
    val prepositionGovernment: List<PrepositionGovernmentDtoV1> = emptyList(),
    @SerialName("complementation") val complementation: List<String> = emptyList(),
    @SerialName("usage_labels") val usageLabels: List<UsageLabelDtoV1> = emptyList(),
    @SerialName("usage_note") val usageNote: String? = null,
    @SerialName("grammar_tags") val grammarTags: List<GrammarTagDtoV1> = emptyList(),
    @SerialName("irregular_forms") val irregularForms: IrregularFormsDtoV1? = null,
)

@Serializable
public data class UnitComponentDtoV1(
    @SerialName("text") val text: String,
    @SerialName("role") val role: String,
    @SerialName("salience") val salience: String? = null,
)

/**
 * One derivative in the [EnrichmentItemV1.wordFamily]: a dictionary word formed
 * from the same root as `base_lemma`, tagged with its part of speech — the
 * lemma↔derivative relationship the app surfaces.
 */
@Serializable
public data class WordFamilyEntryDtoV1(
    @SerialName("lemma") val lemma: String,
    @SerialName("unit_type") val unitType: String,
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

/**
 * Wire shape of a single example: a sentence with optional native-language
 * translation and per-segment alignment. Providers and the curated layer emit
 * this shape; the mapper translates it into [EnrichmentExample] at the AI seam.
 */
@Serializable
public data class EnrichmentExampleV1(
    @SerialName("sentence") val sentence: String,
    @SerialName("translation") val translation: String? = null,
    @SerialName("alignment") val alignment: List<AlignmentChunkV1> = emptyList(),
)

@Serializable
public data class AlignmentChunkV1(
    @SerialName("source") val source: String,
    @SerialName("target") val target: String,
)
