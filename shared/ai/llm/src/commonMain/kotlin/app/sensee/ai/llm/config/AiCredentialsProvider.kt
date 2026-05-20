package app.sensee.ai.llm.config

/**
 * Supplies the API key for the LLM provider. Returns `null` when the user has
 * not configured one — that is a normal state the seam degrades into
 * (`Unavailable`), not an error.
 */
public fun interface AiCredentialsProvider {
    public suspend fun apiKey(): String?
}

public class StaticAiCredentialsProvider(
    private val apiKey: String?,
) : AiCredentialsProvider {
    override suspend fun apiKey(): String? = apiKey
}
