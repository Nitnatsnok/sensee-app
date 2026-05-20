package app.sensee.ai.fixture

import app.sensee.ai.core.AiEnrichmentClient
import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.core.EnrichmentSuggestion
import dev.zacsweers.metro.Inject

/**
 * Zero-config offline implementation of the AI seam. The product is fully
 * usable with no API key: the capture wizard gets deterministic, plausible
 * candidates so the flow is demonstrable end-to-end. The router selects this
 * when no provider key is configured — features never know which answered.
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
            suggestions =
                listOf(
                    EnrichmentSuggestion(
                        translation = "[$term]",
                        surfaceForm = term,
                        unitType = "phrase",
                        baseLemma = term,
                        explanation = "Example sense of \"$term\" (offline fixture).",
                        examples = listOf("This is a sample sentence using [[$term]]."),
                    ),
                ),
        )
    }
}
