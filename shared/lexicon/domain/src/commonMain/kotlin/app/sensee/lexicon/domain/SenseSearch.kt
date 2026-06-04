package app.sensee.lexicon.domain

/**
 * A free-text query over the canonical sense store. [text] is matched
 * case-insensitively against a sense's surface form, translation, and lemma key.
 * [status] is the only filter in this slice; richer filters (CEFR, unit type,
 * origin) are planned behind the same port. [limit] caps the ranked result size
 * and must be non-negative.
 */
public data class SenseQuery(
    val text: String,
    val status: SenseStatus? = null,
    val limit: Int = DEFAULT_LIMIT,
) {
    init {
        require(limit >= 0) { "SenseQuery.limit must be non-negative, was $limit" }
    }

    public companion object {
        public const val DEFAULT_LIMIT: Int = 20
    }
}

/**
 * Read-side search over the canonical sense store, kept separate from
 * [SenseReadRepository] so a consumer that only searches does not take on the
 * by-id / by-key read surface. The shipped implementation matches in memory over
 * the stored senses (surface form, translation, lemma key); a backend search
 * (FTS / semantic) swaps in behind this same port.
 */
public interface SearchPort {
    public suspend fun search(query: SenseQuery): List<StoredSense>
}
