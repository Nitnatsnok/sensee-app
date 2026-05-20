package app.sensee.ai.integration

import app.sensee.ai.llm.config.AiCredentialsProvider
import app.sensee.settings.domain.UserSettingsRepository
import dev.zacsweers.metro.Inject

/**
 * The LLM key comes from device-scoped user settings. A blank/absent key is a
 * normal state — the LLM client degrades to `Unavailable` and the router falls
 * back to the offline fixture.
 */
@Inject
public class SettingsBackedAiCredentialsProvider(
    private val settings: UserSettingsRepository,
) : AiCredentialsProvider {
    override suspend fun apiKey(): String? =
        settings
            .readSettings()
            .ai.aiApiKey
            ?.takeIf { it.isNotBlank() }
}
