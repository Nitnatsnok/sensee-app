package app.sensee.ai.llm.config

/**
 * Provider-agnostic config for any OpenAI-compatible chat-completions endpoint
 * (OpenAI, Azure, OpenRouter, local servers). The seam does not depend on a
 * specific vendor — only on this shape.
 */
public data class LlmConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val model: String = DEFAULT_MODEL,
    // Opt-in: send the EnrichmentSchema as a `json_schema` response_format
    // instead of plain `json_object`. Off by default — only the endpoint owner
    // knows whether the configured provider/model honors it; providers that
    // ignore the parameter are unaffected, and the seam degrades a hard
    // rejection to a first-class Unavailable result anyway (ADR-005).
    val structuredOutput: Boolean = false,
) {
    public companion object {
        public const val DEFAULT_BASE_URL: String = "https://api.openai.com/"
        public const val DEFAULT_MODEL: String = "gpt-4o-mini"
    }
}
