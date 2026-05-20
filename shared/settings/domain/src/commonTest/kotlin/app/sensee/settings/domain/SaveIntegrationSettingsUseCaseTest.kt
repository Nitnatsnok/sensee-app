package app.sensee.settings.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SaveIntegrationSettingsUseCaseTest {
    private class FakeSettings(
        var snapshot: UserSettingsSnapshot = UserSettingsSnapshot(),
    ) : UserSettingsRepository {
        override fun observeSettings(scope: UserSettingsScope): Flow<UserSettingsSnapshot> = flowOf(snapshot)

        override suspend fun readSettings(scope: UserSettingsScope): UserSettingsSnapshot = snapshot

        override suspend fun updateSettings(
            scope: UserSettingsScope,
            transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
        ): UserSettingsSnapshot = transform(snapshot).also { snapshot = it }
    }

    private fun draft(
        aiApiKey: String = "sk-ai",
        aiProvider: AiProvider = AiProvider.OpenAi,
        aiModel: String = "gpt-4o",
        ttsApiKey: String = "",
        ttsProvider: TtsProvider = TtsProvider.OpenAi,
        ttsModel: String = "tts-1",
        ttsVoiceId: String = "alloy",
        ttsSeparateKey: Boolean = false,
    ) = IntegrationSettingsDraft(
        aiApiKey,
        aiProvider,
        aiModel,
        ttsApiKey,
        ttsProvider,
        ttsModel,
        ttsVoiceId,
        ttsSeparateKey,
    )

    @Test
    fun `invoke trims values and maps blank fields to unconfigured`() =
        runTest {
            val settings = FakeSettings()

            val ai =
                SaveIntegrationSettingsUseCase(settings)(
                    draft(
                        aiApiKey = "  sk-abc  ",
                        aiProvider = AiProvider.OpenRouter,
                        aiModel = "   ",
                        ttsApiKey = "tts-xyz",
                        ttsProvider = TtsProvider.OpenAi,
                        ttsModel = "",
                        ttsVoiceId = "  alloy  ",
                    ),
                )

            assertEquals("sk-abc", ai.aiApiKey)
            assertEquals(AiProvider.OpenRouter, ai.aiProvider)
            assertNull(ai.aiModel)
            assertEquals("tts-xyz", ai.ttsApiKey)
            assertNull(ai.ttsModel)
            assertEquals("alloy", ai.ttsVoiceId)
            assertEquals(ai, settings.snapshot.ai)
        }

    @Test
    fun `invoke persists the AI key as the effective TTS key while inheriting`() =
        runTest {
            val settings = FakeSettings()

            SaveIntegrationSettingsUseCase(settings)(
                draft(
                    aiApiKey = "sk-shared",
                    aiProvider = AiProvider.OpenAi,
                    // TTS key field is hidden while inheriting, so it arrives blank.
                    ttsApiKey = "",
                    ttsProvider = TtsProvider.OpenAi,
                    ttsSeparateKey = false,
                ),
            )

            assertEquals("sk-shared", settings.snapshot.ai.ttsApiKey)
            assertEquals(false, settings.snapshot.ai.ttsSeparateKey)
        }

    @Test
    fun `invoke keeps a separate TTS key and does not overwrite it with the AI key`() =
        runTest {
            val settings = FakeSettings()

            SaveIntegrationSettingsUseCase(settings)(
                draft(
                    aiApiKey = "sk-ai",
                    aiProvider = AiProvider.OpenAi,
                    ttsApiKey = "sk-tts-separate",
                    ttsProvider = TtsProvider.OpenAi,
                    ttsSeparateKey = true,
                ),
            )

            assertEquals("sk-tts-separate", settings.snapshot.ai.ttsApiKey)
            assertEquals(true, settings.snapshot.ai.ttsSeparateKey)
        }
}
