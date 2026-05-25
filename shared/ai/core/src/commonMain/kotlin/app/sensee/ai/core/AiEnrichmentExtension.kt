package app.sensee.ai.core

/**
 * Response-side seam (ADR-005): adds a *new wire field* to the provider
 * response and threads its opaque JsonElement to [EnrichmentSuggestion.extensions].
 * Orthogonal to [EnrichmentRequestModifier]; no live extensions yet, infra only.
 */
public interface AiEnrichmentExtension {
    public val id: String

    /** Wire field names this extension owns; must be disjoint with [EnrichmentItemV1]. */
    public val ownedKeys: Set<String>

    /** Schema fragments spliced into the item schema next to the built-in fields. */
    public fun fields(): List<EnrichmentSchema.Field>
}
