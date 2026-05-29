package app.sensee.verification.core

import kotlinx.serialization.Serializable

/**
 * Per-source observations of one claim (existence, POS, CEFR, ...). Multiple
 * providers may answer the same question; the aggregator carries every
 * answer with attribution rather than silently picking one. A consumer reads
 * either [consensus] (the agreed value when all sources agree) or iterates
 * [observations] to render the disagreement.
 */
@Serializable
public data class EvidenceSet<T>(
    val observations: List<Observation<T>>,
) {
    /**
     * The single agreed value, or `null` when empty or in conflict. For a
     * nullable [T] an agreed `null` is indistinguishable from "no consensus";
     * pair with [hasConflict] when that difference matters.
     */
    public val consensus: T?
        get() {
            if (observations.isEmpty()) return null
            val distinct = observations.map { it.value }.toSet()
            return if (distinct.size == 1) distinct.single() else null
        }

    public val hasConflict: Boolean
        get() = observations.map { it.value }.toSet().size > 1

    public companion object {
        private val EMPTY: EvidenceSet<Nothing> = EvidenceSet(emptyList())

        @Suppress("UNCHECKED_CAST")
        public fun <T> empty(): EvidenceSet<T> = EMPTY as EvidenceSet<T>
    }
}

@Serializable
public data class Observation<T>(
    val value: T,
    val source: LexicalSourceRef,
    val confidence: Confidence,
)

/** Coarse, prompt-safe confidence. Adapters can map their score to this scale. */
@Serializable
public enum class Confidence {
    Low,
    Medium,
    High,
}
