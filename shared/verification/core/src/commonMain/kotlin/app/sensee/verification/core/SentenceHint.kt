package app.sensee.verification.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Neutral mirror of the feature's `StudiedSentence`. The seam never imports
 * grammar/domain types directly (ADR-005 boundary); the feature converts on
 * the way in and on the way back.
 *
 * Multiple [Segment.Target] segments interleaved with [Segment.Text] are
 * first-class: a split phrasal verb like `turn it on` arrives as
 * `Target("turn") · Text(" it ") · Target("on")` and the topology must
 * survive every rewrite the seam returns. Adapters that flatten to a string
 * for processing must reattach the Target topology before producing a
 * [SuggestedAction.RewriteExample] — or set
 * [SuggestedAction.RewriteExample.requiresTargetReannotation] to `true` if
 * they could not.
 */
@Serializable
public data class SentenceHint(
    val segments: List<Segment>,
) {
    init {
        require(segments.isNotEmpty()) { "SentenceHint needs at least one segment" }
    }

    /** All Target-segments in order. For `turn it on` that is ["turn", "on"]. */
    public val targets: List<Segment.Target> = segments.filterIsInstance<Segment.Target>()

    /** Indices of every Target inside [segments] — for adapter-side topology preservation. */
    public val targetSegmentIndices: List<Int> =
        segments.mapIndexedNotNull { index, segment -> index.takeIf { segment is Segment.Target } }

    /** Studied unit as a learner reads it (Target segments joined with a space). */
    public fun studiedUnitDisplay(): String = targets.joinToString(separator = " ") { it.value }

    /** The unmarked flat text, e.g. for shipping to LanguageTool. */
    public fun plainText(): String = segments.joinToString(separator = "") { it.text }

    @Serializable
    public sealed interface Segment {
        public val text: String

        @Serializable
        @SerialName("text")
        public data class Text(
            val value: String,
        ) : Segment {
            override val text: String get() = value
        }

        @Serializable
        @SerialName("target")
        public data class Target(
            val value: String,
        ) : Segment {
            override val text: String get() = value
        }
    }
}
