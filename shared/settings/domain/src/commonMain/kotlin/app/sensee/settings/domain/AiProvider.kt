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
        knownModels = listOf("gpt-5.5", "gpt-5.4-mini", "gpt-4.1-mini"),
    ),
    OpenRouter(
        id = "openrouter",
        displayName = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/",
        knownModels = listOf("openai/gpt-5.5", "openai/gpt-5.4-mini", "openai/gpt-4.1-mini"),
    ),
    ;

    /**
     * Whether this provider/model pair is expected to honor OpenAI-compatible
     * `response_format = json_schema`. The check is intentionally conservative:
     * OpenRouter can proxy models that do not implement the parameter, so it
     * stays on plain JSON mode until model-level capabilities are surfaced.
     */
    public fun supportsJsonSchemaResponseFormat(model: String): Boolean =
        when (this) {
            OpenAi -> model.isOpenAiStructuredOutputModel()
            OpenRouter -> false
        }

    public companion object {
        public val Default: AiProvider = OpenAi

        public fun fromId(id: String?): AiProvider = entries.firstOrNull { it.id == id } ?: Default
    }
}

private val OpenAiStructuredOutputModelPrefixes =
    listOf(
        "gpt-4o",
        "gpt-4.1",
        "gpt-5",
    )

private fun String.isOpenAiStructuredOutputModel(): Boolean {
    val normalized = trim().lowercase()
    return OpenAiStructuredOutputModelPrefixes.any { prefix ->
        normalized == prefix || normalized.startsWith("$prefix-") || normalized.startsWith("$prefix.")
    }
}
