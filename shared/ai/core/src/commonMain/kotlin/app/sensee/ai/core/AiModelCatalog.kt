package app.sensee.ai.core

/**
 * Key verification and model enumeration of the AI seam (ADR-005), one
 * operation. The settings UI verifies a typed key by the same `/v1/models`
 * call that populates the model picker: a valid key yields the chat-suitable
 * model list, a rejected/unreachable provider yields a first-class
 * [AiKeyCheck.Invalid] reason. The key is passed explicitly (the user verifies
 * before saving, so it cannot be read from saved settings). Never throws —
 * availability is first-class, like [AiEnrichmentClient.enrich].
 */
public interface AiModelCatalog {
    public suspend fun verifyKey(
        baseUrl: String,
        apiKey: String,
    ): AiKeyCheck
}

/**
 * Outcome of verifying a provider key against its models endpoint. [Valid]
 * carries the chat-suitable models (possibly empty: a working key whose
 * provider exposes no chat model — the picker then offers manual entry);
 * [Invalid] carries a human-readable reason for the settings UI.
 */
public sealed interface AiKeyCheck {
    public data class Valid(
        val models: List<String>,
    ) : AiKeyCheck

    public data class Invalid(
        val reason: String,
    ) : AiKeyCheck
}
