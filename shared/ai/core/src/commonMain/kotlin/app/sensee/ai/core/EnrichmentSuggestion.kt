package app.sensee.ai.core

import kotlinx.serialization.json.JsonElement

/**
 * A single AI-proposed candidate sense (ADR-001: never canonical without user
 * selection). [extensions] holds [AiEnrichmentExtension] fields keyed by wire
 * name; built-ins never leak here.
 */
public data class EnrichmentSuggestion(
    val translation: String,
    val surfaceForm: String? = null,
    val unitType: String? = null,
    val baseLemma: String? = null,
    val headLemma: String? = null,
    val components: List<UnitComponentHint> = emptyList(),
    val explanation: String? = null,
    val examples: List<EnrichmentExample> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val wordFamily: List<WordFamilyHint> = emptyList(),
    val governedPrepositions: List<PrepositionGovernmentHint> = emptyList(),
    val complementation: List<String> = emptyList(),
    val usageLabels: List<UsageLabelHint> = emptyList(),
    val usageNote: String? = null,
    val grammarTags: List<GrammarTagHint> = emptyList(),
    val irregularForms: IrregularFormsHint? = null,
    val extensions: Map<String, JsonElement> = emptyMap(),
)

/**
 * Usage example with optional translation + per-segment alignment.
 * [sentence] wraps the studied unit in `[[ ]]`.
 */
public data class EnrichmentExample(
    val sentence: String,
    val translation: String? = null,
    val alignment: List<AlignmentChunk> = emptyList(),
)

/** One (source, target) chunk pair within an [EnrichmentExample.alignment]. */
public data class AlignmentChunk(
    val source: String,
    val target: String,
)

/**
 * One component of a multi-word unit. [salience] ranks its significance to the
 * unit's meaning (`primary`/`secondary`/`incidental`) for UI emphasis — e.g.
 * `come` is primary and `across` secondary in "come across".
 */
public data class UnitComponentHint(
    val text: String,
    val role: String,
    val salience: String? = null,
)

/** One derivative ([lemma]) of the sense's base lemma, tagged with its [unitType]. */
public data class WordFamilyHint(
    val lemma: String,
    val unitType: String,
)

/** A neutral (axisId, valueId) usage-nuance pair; resolved/validated at the feature boundary. */
public data class UsageLabelHint(
    val axis: String,
    val value: String,
)

/** A neutral (categoryId, formId) pair; resolved/validated at the feature boundary. */
public data class GrammarTagHint(
    val category: String,
    val form: String,
)

/** Neutral group of interchangeable prepositions for one sense, optional example. */
public data class PrepositionGovernmentHint(
    val alternatives: List<String>,
    val example: String? = null,
)

/**
 * Principal parts of an irregular verb. The mapper drops the whole hint when
 * any one slot is missing.
 */
public data class IrregularFormsHint(
    val base: String,
    val past: String,
    val pastParticiple: String,
)
