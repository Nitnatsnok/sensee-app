package app.sensee.lexicon.domain

import kotlin.math.sqrt

/**
 * A sense's embedding vector with the model identity that produced it. [modelRef]
 * and [dim] (the vector length) together gate comparison: only vectors from the
 * same `(modelRef, dim)` are cosine-comparable, so a model or dimension change
 * never compares apples to oranges. Stored normalized to unit length, so cosine
 * similarity reduces to a dot product. An intentional boundary mirror of the
 * ai-core embedding the provider returns, kept separate so `lexicon/domain` stays
 * dependency-light.
 *
 * A plain class (not `data`): a [FloatArray] has identity equality, so structural
 * `equals`/`copy` would mislead. Compare [values] with `contentEquals`.
 */
public class EmbeddingVector(
    public val values: FloatArray,
    public val modelRef: String,
) {
    public val dim: Int get() = values.size

    init {
        require(values.isNotEmpty()) { "EmbeddingVector must not be empty" }
        require(modelRef.isNotBlank()) { "EmbeddingVector must name its model" }
    }
}

/**
 * A stored sense found cosine-similar to a query vector, with its [score] in
 * `[-1, 1]`. A soft "looks similar" signal for a user choice (reuse vs. create
 * new) — never the hard exact-duplicate check, which `content_key` owns.
 */
public data class SimilarSense(
    val sense: StoredSense,
    val score: Float,
)

/**
 * Cosine similarity of two equal-length vectors, in `[-1, 1]`. Returns `0` when
 * either vector is all-zero (no direction to compare). Accumulates in [Double]
 * for stability over many dimensions, then narrows to [Float].
 */
public fun cosineSimilarity(
    a: FloatArray,
    b: FloatArray,
): Float {
    require(a.size == b.size) { "Vectors must share dimension: ${a.size} vs ${b.size}" }
    var dot = 0.0
    var normA = 0.0
    var normB = 0.0
    for (i in a.indices) {
        val x = a[i].toDouble()
        val y = b[i].toDouble()
        dot += x * y
        normA += x * x
        normB += y * y
    }
    if (normA == 0.0 || normB == 0.0) return 0f
    return (dot / (sqrt(normA) * sqrt(normB))).toFloat()
}

/**
 * Embedding-backed similar-sense lookup over the canonical sense store, kept
 * separate from [SenseReadRepository]/[SenseWriteRepository] so it carries its
 * own `sense_embedding` table and similarity math without widening the core
 * repositories. A backend embedding service swaps in behind this same port.
 */
public interface EmbeddingPort {
    /**
     * Embed [stored] and persist its vector, best-effort. Returns the vector, or
     * `null` when no provider answered (no key / offline / error) — the sense row
     * is left "not embedded" for a later lazy backfill. Never throws for a
     * provider miss. Callers run it *after* the sense row has committed, outside
     * the write transaction and within a bounded budget ([EAGER_EMBED_BUDGET_MS]),
     * so a miss or overrun leaves the sense for a later backfill and never fails
     * or blocks a save.
     */
    public suspend fun embed(stored: StoredSense): EmbeddingVector?

    /**
     * Stored senses whose vector is cosine-similar to [query] at or above
     * [threshold], excluding [excluding], strongest first. Compares only
     * already-embedded senses sharing [query]'s `(modelRef, dim)`.
     */
    public suspend fun findSimilar(
        query: EmbeddingVector,
        excluding: SenseId,
        threshold: Float = DEFAULT_SIMILARITY_THRESHOLD,
    ): List<SimilarSense>

    public companion object {
        /** Cosine threshold above which two senses are offered as "looks similar". */
        public const val DEFAULT_SIMILARITY_THRESHOLD: Float = 0.85f

        /**
         * Eager-embed-on-save ceiling. A confirm or claim embeds within this budget;
         * past it the sense is left "not embedded" for a later lazy backfill, so the
         * save's round-trip never blocks on a slow provider. A confirm batch embeds
         * its senses concurrently, so the budget covers one round of parallel calls,
         * not a per-sense sum.
         */
        public const val EAGER_EMBED_BUDGET_MS: Long = 5_000L
    }
}
