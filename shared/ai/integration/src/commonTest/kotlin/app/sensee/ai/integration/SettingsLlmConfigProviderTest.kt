package app.sensee.ai.integration

import app.sensee.settings.domain.AiProvider
import app.sensee.settings.domain.AiSettings
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsLlmConfigProviderTest {
    @Test
    fun `OpenAI settings enable structured output for supported models`() =
        runTest {
            val config =
                provider(
                    AiSettings(
                        aiProvider = AiProvider.OpenAi,
                        aiModel = "gpt-4.1-mini",
                    ),
                ).config()

            assertEquals("https://api.openai.com/", config.baseUrl)
            assertEquals("gpt-4.1-mini", config.model)
            assertEquals(true, config.structuredOutput)
        }

    @Test
    fun `OpenAI settings keep plain json mode for older unsupported models`() =
        runTest {
            val config =
                provider(
                    AiSettings(
                        aiProvider = AiProvider.OpenAi,
                        aiModel = "gpt-4-turbo",
                    ),
                ).config()

            assertEquals(false, config.structuredOutput)
        }

    @Test
    fun `OpenRouter settings keep plain json mode until model capabilities are explicit`() =
        runTest {
            val config =
                provider(
                    AiSettings(
                        aiProvider = AiProvider.OpenRouter,
                        aiModel = "openai/gpt-4o-mini",
                    ),
                ).config()

            assertEquals("https://openrouter.ai/api/", config.baseUrl)
            assertEquals("openai/gpt-4o-mini", config.model)
            assertEquals(false, config.structuredOutput)
        }

    @Test
    fun `provider default model is used when settings model is blank`() =
        runTest {
            val config =
                provider(
                    AiSettings(
                        aiProvider = AiProvider.OpenAi,
                        aiModel = " ",
                    ),
                ).config()

            assertEquals(AiProvider.OpenAi.knownModels.first(), config.model)
            assertEquals(true, config.structuredOutput)
        }

    private fun provider(ai: AiSettings): SettingsLlmConfigProvider =
        SettingsLlmConfigProvider(FakeSettings(UserSettingsSnapshot(ai = ai)))

    private class FakeSettings(
        private val snapshot: UserSettingsSnapshot,
    ) : UserSettingsRepository {
        override fun observeSettings(scope: UserSettingsScope): Flow<UserSettingsSnapshot> = flowOf(snapshot)

        override suspend fun readSettings(scope: UserSettingsScope): UserSettingsSnapshot = snapshot

        override suspend fun updateSettings(
            scope: UserSettingsScope,
            transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
        ): UserSettingsSnapshot = transform(snapshot)
    }
}
