package app.sensee.ai.core.grounding

import app.sensee.ai.core.request.GroundingModifier

/**
 * Neutral dictionary/corpus grounding fed into a provider before it picks
 * senses. The seam keeps this provider-agnostic by design: the orchestrator
 * (a feature use case) assembles it from a verification report and gates
 * the contents by the source's `usableAsLlmContext` license flag, so an
 * adapter that disallows third-party-LLM context cannot leak its content
 * here. ("evidence" is the verification side's term; on the AI side this is
 * the grounding channel.)
 *
 * Treatment of grounding by the prompt is explicit in [GroundingModifier]:
 * factual fields (lemma, parts of speech, IPA, CEFR, frequency) are
 * ground-truth-shaped; sense summaries are non-exhaustive — the model may
 * still add a missing common sense.
 */
public data class EnrichmentGrounding(
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
    val sources: List<GroundingSourceAttribution> = emptyList(),
    val confidence: GroundingConfidence = GroundingConfidence.Medium,
)

/** Three-step prompt-safe confidence; mapped from the verifier's per-claim confidence. */
public enum class GroundingConfidence {
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

/**
 * Intentional boundary mirror of `verification.core.UnitComponentFact`: both are
 * zero-dependency leaves with no shared ancestor. A common type would force a
 * shared dependency onto the deliberately dependency-free `ai.core` and
 * `verification.core` boundaries, so the duplication is by design (plan §3).
 */
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

public data class GroundingSourceAttribution(
    val sourceId: String,
    val displayName: String,
)
