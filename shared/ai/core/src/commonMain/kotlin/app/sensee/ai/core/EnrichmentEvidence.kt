package app.sensee.ai.core

/**
 * Neutral dictionary/corpus evidence fed into a provider before it picks
 * senses. The seam keeps this provider-agnostic by design: the orchestrator
 * (a feature use case) assembles it from a verification report and gates
 * the contents by the source's `usableAsLlmContext` license flag, so an
 * adapter that disallows third-party-LLM context cannot leak its content
 * here.
 *
 * Treatment of evidence by the prompt is explicit in [EvidenceModifier]:
 * factual fields (lemma, parts of speech, IPA, CEFR, frequency) are
 * ground-truth-shaped; sense summaries are non-exhaustive — the model may
 * still add a missing common sense.
 */
public data class EnrichmentEvidence(
    val normalized: NormalizationFact? = null,
    val lemma: String? = null,
    val entryType: String? = null,
    val unit: UnitFact? = null,
    val knownPartsOfSpeech: List<String> = emptyList(),
    val knownSenseSummaries: List<SenseSummary> = emptyList(),
    val cefr: String? = null,
    val frequency: FrequencyFact? = null,
    val pronunciations: List<PronunciationFact> = emptyList(),
    val family: FamilyFact? = null,
    val sources: List<EvidenceSourceAttribution> = emptyList(),
    val confidence: EvidenceConfidence = EvidenceConfidence.Medium,
)

/** Three-step prompt-safe confidence; mapped from the verifier's per-claim confidence. */
public enum class EvidenceConfidence {
    Low,
    Medium,
    High,
}

public data class NormalizationFact(
    val canonical: String,
    val kind: String,
)

public data class UnitFact(
    val displayForm: String,
    val entryType: String,
    val headLemma: String? = null,
    val components: List<UnitComponentFact> = emptyList(),
)

public data class UnitComponentFact(
    val text: String,
    val role: String,
)

public data class SenseSummary(
    val pos: String? = null,
    val shortLabel: String? = null,
    val cefr: String? = null,
)

public data class FrequencyFact(
    val zipf: Double? = null,
    val band: String? = null,
)

public data class PronunciationFact(
    val accent: String? = null,
    val ipa: String? = null,
)

public data class FamilyFact(
    val head: String,
    val siblings: List<FamilySiblingFact>,
    val truncated: Boolean = false,
)

public data class FamilySiblingFact(
    val displayForm: String,
    val entryType: String,
)

public data class EvidenceSourceAttribution(
    val sourceId: String,
    val displayName: String,
)
