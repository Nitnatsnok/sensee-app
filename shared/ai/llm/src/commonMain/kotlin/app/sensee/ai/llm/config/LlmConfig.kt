package app.sensee.ai.llm.config

/**
 * Provider-agnostic config for any OpenAI-compatible chat-completions endpoint
 * (OpenAI, Azure, OpenRouter, local servers). The seam does not depend on a
 * specific vendor — only on this shape.
 */
public data class LlmConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val model: String = DEFAULT_MODEL,
    // Opt-in at the low-level config boundary: send the EnrichmentSchema as a
    // `json_schema` response_format instead of plain `json_object`. The
    // settings-backed production provider enables it only for known-compatible
    // provider/model pairs; raw config defaults to the most portable mode.
    val structuredOutput: Boolean = false,
) {
    public companion object {
        public const val DEFAULT_BASE_URL: String = "https://api.openai.com/"
        public const val DEFAULT_MODEL: String = "gpt-4o-mini"
    }
}
