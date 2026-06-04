package app.sensee.ai.core.contract

import app.sensee.ai.core.model.EnrichmentSuggestion

/**
 * Outcome of an enrichment call. Availability is first-class (ADR-005):
 * unavailability and partiality are normal states the wizard degrades into,
 * not exceptions.
 *
 * Invariant: [EnrichmentAvailability.Unavailable] implies [suggestions] is empty.
 */
public data class EnrichmentResult(
    val availability: EnrichmentAvailability,
    val suggestions: List<EnrichmentSuggestion> = emptyList(),
) {
    init {
        require(availability !is EnrichmentAvailability.Unavailable || suggestions.isEmpty()) {
            "Unavailable enrichment must carry no suggestions"
        }
    }

    public companion object {
        public fun unavailable(reason: String): EnrichmentResult =
            EnrichmentResult(EnrichmentAvailability.Unavailable(reason))
    }
}

public sealed interface EnrichmentAvailability {
    public data object Available : EnrichmentAvailability

    public data class Degraded(
        val reason: String,
    ) : EnrichmentAvailability

    public data class Unavailable(
        val reason: String,
    ) : EnrichmentAvailability
}
