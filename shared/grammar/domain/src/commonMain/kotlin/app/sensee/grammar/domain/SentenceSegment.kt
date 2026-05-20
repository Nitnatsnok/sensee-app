package app.sensee.grammar.domain

/**
 * An example sentence as structured segments, never a flat string (ADR-001,
 * same "structured, not string" rule as [SurfaceForm]). The studied unit is
 * marked as [SentenceSegment.Target] by the producer (the AI knows the exact
 * span), so practice can highlight it or mask it for cloze WITHOUT fragile
 * substring matching — which fails on inflected forms (come/came), separated
 * phrasals (turn it on) and false substrings.
 */
public data class StudiedSentence(
    val segments: List<SentenceSegment>,
) {
    init {
        require(segments.isNotEmpty()) { "StudiedSentence needs at least one segment" }
    }

    /** Plain rendering with the target inlined (no markers). */
    public fun plainText(): String =
        segments.joinToString("") { segment ->
            when (segment) {
                is SentenceSegment.Text -> segment.value
                is SentenceSegment.Target -> segment.value
            }
        }

    public val target: String?
        get() = segments.filterIsInstance<SentenceSegment.Target>().firstOrNull()?.value

    /** Inverse of [parse]: the target span re-wrapped in `[[ ]]` for transport/storage. */
    public fun marked(): String =
        segments.joinToString("") { segment ->
            when (segment) {
                is SentenceSegment.Text -> segment.value
                is SentenceSegment.Target -> "$OPEN${segment.value}$CLOSE"
            }
        }

    public companion object {
        private const val OPEN = "[["
        private const val CLOSE = "]]"

        /**
         * Parses a sentence whose studied unit is wrapped in `[[ ]]`, e.g.
         * `She [[came across]] the letters.` Text outside the markers becomes
         * [SentenceSegment.Text]; the marked span becomes [SentenceSegment.Target].
         * Unmarked input is a single [SentenceSegment.Text] (still valid — the
         * producer simply did not mark a target).
         */
        public fun parse(raw: String): StudiedSentence {
            val open = raw.indexOf(OPEN)
            val close = if (open < 0) -1 else raw.indexOf(CLOSE, open + OPEN.length)
            if (open < 0 || close < 0) {
                return StudiedSentence(listOf(SentenceSegment.Text(raw)))
            }
            val before = raw.substring(0, open)
            val targetText = raw.substring(open + OPEN.length, close).trim()
            val after = raw.substring(close + CLOSE.length)
            return StudiedSentence(
                buildList {
                    if (before.isNotEmpty()) add(SentenceSegment.Text(before))
                    add(SentenceSegment.Target(targetText))
                    if (after.isNotEmpty()) add(SentenceSegment.Text(after))
                },
            )
        }
    }
}

public sealed interface SentenceSegment {
    /** A non-studied part of the sentence. */
    public data class Text(
        val value: String,
    ) : SentenceSegment

    /** The studied lexical unit occurrence (highlight / cloze blank). */
    public data class Target(
        val value: String,
    ) : SentenceSegment
}
