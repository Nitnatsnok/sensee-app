package app.sensee.ai.core

/**
 * The AI enrichment seam. Features depend only on this interface — never on a
 * vendor SDK (ADR-005). The curated-backed implementation is the default for
 * covered lemmas so the product works with zero configuration; a real LLM
 * provider is opt-in via settings.
 */
public interface AiEnrichmentClient {
    public suspend fun enrich(request: EnrichmentRequest): EnrichmentResult
}
