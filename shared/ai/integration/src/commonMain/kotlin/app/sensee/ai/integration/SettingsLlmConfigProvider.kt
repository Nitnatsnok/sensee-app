package app.sensee.ai.integration

import app.sensee.ai.llm.config.LlmConfig
import app.sensee.ai.llm.config.LlmConfigProvider
import app.sensee.settings.domain.UserSettingsRepository
import dev.zacsweers.metro.Inject

/**
 * Resolves the LLM endpoint from device-scoped settings per request, so a single
 * long-lived client picks up a base URL / model the user changes at runtime.
 */
@Inject
public class SettingsLlmConfigProvider(
    private val settings: UserSettingsRepository,
) : LlmConfigProvider {
    override suspend fun config(): LlmConfig {
        val ai = settings.readSettings().ai
        val provider = ai.aiProvider
        val model =
            ai.aiModel?.takeIf { it.isNotBlank() }
                ?: provider.knownModels.firstOrNull()
                ?: LlmConfig.DEFAULT_MODEL
        return LlmConfig(
            baseUrl = provider.baseUrl,
            model = model,
            structuredOutput = provider.supportsJsonSchemaResponseFormat(model),
        )
    }
}
