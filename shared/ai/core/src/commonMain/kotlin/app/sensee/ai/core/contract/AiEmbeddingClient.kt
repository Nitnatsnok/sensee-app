package app.sensee.ai.core.contract

/**
 * The text-embedding seam: turns a piece of text into a dense vector. Features
 * depend only on this interface — never a vendor SDK (ADR-005), symmetric to
 * [AiEnrichmentClient]. The vector rides its own call, not the enrichment wire
 * (ADR-006): one embedding path, [embed].
 *
 * A `null` result is a normal degraded state — no API key, offline, or a
 * malformed provider response — never an exception thrown across the seam. The
 * caller treats it as "not embedded" and tries again later.
 *
 * Unlike [EnrichmentResult], this returns a nullable vector rather than a
 * first-class availability type: a vector either arrives or it does not — there
 * is no partial or user-facing "degraded" state to model, and embedding runs in
 * the background with no screen to surface a reason. `null` is that one outcome.
 */
public interface AiEmbeddingClient {
    public suspend fun embed(text: String): Embedding?
}

/**
 * A provider-computed embedding: the raw [values] and the [model] that produced
 * them. Stays provider- and feature-agnostic — no lexical types — so `ai/core`
 * remains a neutral leaf; the lexicon maps it into its own `EmbeddingVector`, an
 * intentional boundary mirror that keeps `ai/core` dependency-free.
 *
 * A plain class (not `data`): a [FloatArray] has identity equality, so structural
 * `equals`/`copy` would be misleading. Compare [values] with `contentEquals`.
 */
public class Embedding(
    public val values: FloatArray,
    public val model: String,
) {
    init {
        require(values.isNotEmpty()) { "Embedding must carry at least one value" }
        require(model.isNotBlank()) { "Embedding must name its model" }
    }
}
