package app.sensee.ai.core

/**
 * A single AI-proposed sense for a term. Always a candidate, never canonical
 * content (ADR-001): consumers map this into their own candidate model and
 * require explicit user selection before it becomes domain data.
 *
 * Neutral by design — no provider shape, no feature/grammar domain type. The
 * structured surface form, unit type and grammar tags are carried as plain
 * ids/strings; the feature boundary mapper turns them into structured domain
 * values and drops anything that violates the grammar invariant.
 */
public data class EnrichmentSuggestion(
    val translation: String,
    val surfaceForm: String? = null,
    val unitType: String? = null,
    val baseLemma: String? = null,
    val explanation: String? = null,
    val examples: List<String> = emptyList(),
    val governedPrepositions: List<PrepositionGovernmentHint> = emptyList(),
    val complementation: List<String> = emptyList(),
    val usageLabels: List<UsageLabelHint> = emptyList(),
    val usageNote: String? = null,
    val grammarTags: List<GrammarTagHint> = emptyList(),
    val irregularForms: IrregularFormsHint? = null,
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

/** Neutral principal parts of an irregular verb (e.g. come / came / come). */
public data class IrregularFormsHint(
    val base: String,
    val past: String,
    val pastParticiple: String,
)
