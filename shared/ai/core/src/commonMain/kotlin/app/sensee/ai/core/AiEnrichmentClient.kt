package app.sensee.ai.core

/**
 * The AI enrichment seam. Features depend only on this interface — never on a
 * vendor SDK (ADR-005). The fixture-backed implementation is the default so the
 * product works with zero configuration; a real provider is opt-in via settings.
 */
public interface AiEnrichmentClient {
    public suspend fun enrich(request: EnrichmentRequest): EnrichmentResult
}
