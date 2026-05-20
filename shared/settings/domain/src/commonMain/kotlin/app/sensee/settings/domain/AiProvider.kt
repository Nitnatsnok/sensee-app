package app.sensee.settings.domain

/**
 * A compatible AI provider chosen from a fixed list (ADR-005): the provider
 * predefines its base URL, so the user never types one. [knownModels] is the
 * offline fallback used when the provider's models API is unreachable; the live
 * list is fetched through the AI seam's model-catalog operation.
 */
public enum class AiProvider(
    public val id: String,
    public val displayName: String,
    public val baseUrl: String,
    public val knownModels: List<String>,
) {
    OpenAi(
        id = "openai",
        displayName = "OpenAI",
        baseUrl = "https://api.openai.com/",
        knownModels = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini"),
    ),
    OpenRouter(
        id = "openrouter",
        displayName = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/",
        knownModels = listOf("openai/gpt-4o-mini", "anthropic/claude-3.5-sonnet"),
    ),
    ;

    public companion object {
        public val Default: AiProvider = OpenAi

        public fun fromId(id: String?): AiProvider = entries.firstOrNull { it.id == id } ?: Default
    }
}
