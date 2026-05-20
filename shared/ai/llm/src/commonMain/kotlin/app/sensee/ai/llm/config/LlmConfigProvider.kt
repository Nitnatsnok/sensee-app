package app.sensee.ai.llm.config

/**
 * Resolves the LLM endpoint per request so a single long-lived [HttpClient]
 * (Ktor best practice) can serve a base URL / model that the user may change
 * at runtime in settings.
 */
public fun interface LlmConfigProvider {
    public suspend fun config(): LlmConfig
}

public class StaticLlmConfigProvider(
    private val config: LlmConfig = LlmConfig(),
) : LlmConfigProvider {
    override suspend fun config(): LlmConfig = config
}
