package app.sensee.ai.fixture

import app.sensee.ai.core.AiEnrichmentClient
import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.ai.core.EnrichmentResult
import dev.zacsweers.metro.Inject

/**
 * Zero-config offline implementation of the AI seam. The product is fully
 * usable with no API key: the capture flow gets deterministic, plausible
 * candidates so it is demonstrable end-to-end. The router selects this when no
 * provider key is configured — features never know which answered.
 *
 * Well-known demo terms expand into a realistic multi-sense set (see
 * [FixtureEnrichmentData]); an unknown term gets a single generic suggestion.
 */
@Inject
public class FixtureAiEnrichmentClient : AiEnrichmentClient {
    override suspend fun enrich(request: EnrichmentRequest): EnrichmentResult {
        val term = request.term.trim()
        if (term.isEmpty()) {
            return EnrichmentResult(EnrichmentAvailability.Available)
        }
        return EnrichmentResult(
            availability = EnrichmentAvailability.Available,
            suggestions = FixtureEnrichmentData.suggestionsFor(term),
        )
    }
}
