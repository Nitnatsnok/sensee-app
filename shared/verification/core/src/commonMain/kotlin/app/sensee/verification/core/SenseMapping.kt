package app.sensee.verification.core

import kotlinx.serialization.Serializable

/**
 * Verifier's view of how the orchestrator's [SenseHint]s line up against the
 * dictionary sense inventory. Three buckets so the orchestrator never has to
 * guess whether silence means "I matched it" or "I do not handle senses":
 * matched senses ride in [matched]; AI senses the verifier could not pin to
 * any dictionary sense are in [unmatchedAiSenses]; dictionary senses the AI
 * never proposed are in [extraDictionarySenses] for an optional "you may
 * also want…" surface.
 */
@Serializable
public data class SenseMapping(
    val matched: List<MatchedSense> = emptyList(),
    val unmatchedAiSenses: List<String> = emptyList(),
    val extraDictionarySenses: List<DictionarySenseSummary> = emptyList(),
    val confidence: Confidence = Confidence.Low,
) {
    public companion object {
        public val EMPTY: SenseMapping = SenseMapping()
    }
}

@Serializable
public data class MatchedSense(
    val aiSenseHintId: String,
    val dictionarySenseRef: LexicalSourceRef,
    val similarity: Double,
    val partOfSpeechAgrees: Boolean,
    val cefr: CefrLevel? = null,
    val labels: List<UsageLabelHint> = emptyList(),
)

@Serializable
public data class DictionarySenseSummary(
    val ref: LexicalSourceRef,
    val pos: PartOfSpeechHint? = null,
    /**
     * Short safe-to-store label. Carrying a full gloss verbatim is gated by
     * the source's [LicensePolicy.storeContentAllowed]; adapters that cannot
     * store glosses leave this `null` and the orchestrator renders by ref
     * only.
     */
    val shortLabel: String? = null,
    val cefr: CefrLevel? = null,
)

/** Neutral mirror of the feature's `UsageLabel`; resolved at the boundary. */
@Serializable
public data class UsageLabelHint(
    val axis: String,
    val value: String,
)
