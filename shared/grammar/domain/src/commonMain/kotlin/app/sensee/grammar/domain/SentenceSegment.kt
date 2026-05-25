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
         * Parses a sentence whose studied units are wrapped in `[[ ]]`, e.g.
         * `She [[comes across]] [[as]] confident.` Every `[[ ]]` span becomes a
         * [SentenceSegment.Target]; surrounding text becomes
         * [SentenceSegment.Text]. Unmarked input is a single [SentenceSegment.Text]
         * (still valid — the producer simply did not mark a target). An opening
         * `[[` without a matching `]]` is treated as literal text from that point
         * on (do not silently drop the rest of the sentence).
         */
        public fun parse(raw: String): StudiedSentence {
            val segments = mutableListOf<SentenceSegment>()
            var cursor = 0
            while (true) {
                val open = raw.indexOf(OPEN, cursor)
                val close = if (open < 0) -1 else raw.indexOf(CLOSE, open + OPEN.length)
                if (open < 0 || close < 0) {
                    // No more markers (or an unbalanced opener): keep the tail
                    // as plain text and finish.
                    if (cursor < raw.length) segments.add(SentenceSegment.Text(raw.substring(cursor)))
                    break
                }
                if (open > cursor) segments.add(SentenceSegment.Text(raw.substring(cursor, open)))
                segments.add(SentenceSegment.Target(raw.substring(open + OPEN.length, close).trim()))
                cursor = close + CLOSE.length
            }
            if (segments.isEmpty()) segments.add(SentenceSegment.Text(raw))
            return StudiedSentence(segments)
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
