package app.sensee.ai.core.contract

import app.sensee.ai.core.model.CefrEnrichmentExtension
import app.sensee.ai.core.model.EnrichmentSuggestion
import app.sensee.ai.core.request.EnrichmentRequestModifier
import app.sensee.ai.core.request.EnrichmentSchema
import app.sensee.ai.core.wire.EnrichmentItemV1
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Response-side seam (ADR-005): adds a *new wire field* to the provider
 * response and threads its opaque JsonElement to [EnrichmentSuggestion.extensions].
 * Orthogonal to [EnrichmentRequestModifier]; [CefrEnrichmentExtension] is the
 * first live extension.
 */
public interface AiEnrichmentExtension {
    public val id: String

    /** Wire field names this extension owns; must be disjoint with [EnrichmentItemV1]. */
    public val ownedKeys: Set<String>

    /** Schema fragments spliced into the item schema next to the built-in fields. */
    public fun fields(): List<EnrichmentSchema.Field>
}

/**
 * Owned keys across this extension set, minus any that collide with a built-in
 * wire field. Built-ins win on collision (ADR-006), so a misconfigured
 * extension can never reclaim a wire field through the opaque bucket.
 */
public fun Set<AiEnrichmentExtension>.externalKeys(): Set<String> =
    flatMapTo(mutableSetOf()) { it.ownedKeys }.apply { removeAll(EnrichmentSchema.builtInFieldNames) }

/**
 * Pulls this item's [externalKeys] values into the opaque bucket threaded to
 * [EnrichmentSuggestion.extensions]. Shared by the LLM and curated providers so
 * both surface extensions identically and the same collision rule applies.
 */
public fun Set<AiEnrichmentExtension>.extractItemExtensions(item: JsonObject): Map<String, JsonElement> {
    val keys = externalKeys()
    if (keys.isEmpty()) return emptyMap()
    return keys.mapNotNull { key -> item[key]?.let { key to it } }.toMap()
}
