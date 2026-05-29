package app.sensee.ai.core

/**
 * Input for an AI enrichment call. Provider-agnostic and feature-agnostic: the
 * seam never sees vendor types nor feature domain types.
 *
 * The input language is NOT supplied — it is auto-detected by the provider
 * (ADR-001/ADR-005): the request carries the raw term plus the fixed
 * study/native pair (en/ru now). For Russian input the provider returns the
 * matching English senses with a Russian equivalent; the response shape is
 * identical regardless of input language.
 *
 * [topicPreferences] are neutral topic keywords (e.g. "travel and tourism")
 * the provider may use to steer example sentences toward the learner's
 * interests. An empty list means no steering; the keywords never affect sense
 * splitting, translation or grammar.
 */
public data class EnrichmentRequest(
    val term: String,
    val studyLanguageTag: String = "en",
    val nativeLanguageTag: String = "ru",
    val userNote: String? = null,
    val senseCoverage: SenseCoverage = SenseCoverage.Common,
    val topicPreferences: List<String> = emptyList(),
    val evidence: EnrichmentEvidence? = null,
)

/**
 * How much of the input's sense inventory to return (ADR-005). Not a minItems
 * floor — a genuinely monosemous unit still returns one item under any value;
 * this only shapes the prompt and whether a suspicious one-item answer is
 * re-checked (see the enrichment client's retry).
 */
public enum class SenseCoverage {
    /** Only the most important sense(s); for a quick add, no retry. */
    Minimal,

    /** All common learner-relevant senses; usually 2–5 for polysemous units. */
    Common,

    /** As many useful distinct dictionary senses as practical, still no rare/obsolete. */
    Comprehensive,
}
