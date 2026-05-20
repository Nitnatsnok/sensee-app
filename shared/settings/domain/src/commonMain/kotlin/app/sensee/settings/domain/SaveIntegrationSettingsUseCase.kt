package app.sensee.settings.domain

import dev.zacsweers.metro.Inject

/**
 * The form values to persist as AI/TTS integration settings. [ttsSeparateKey]
 * is the user's opt-out from the OpenAI key-inheritance rule (see
 * [ttsInheritsAiKey]); blank key/model fields mean "not configured".
 */
public data class IntegrationSettingsDraft(
    val aiApiKey: String,
    val aiProvider: AiProvider,
    val aiModel: String,
    val ttsApiKey: String,
    val ttsProvider: TtsProvider,
    val ttsModel: String,
    val ttsVoiceId: String,
    val ttsSeparateKey: Boolean,
)

/**
 * Persists integration settings, applying the OpenAI key-inheritance rule:
 * while TTS inherits the AI key the TTS field is hidden and arrives blank, so
 * the AI key is stored as the effective TTS key (TTS routing reads only
 * [AiSettings.ttsApiKey]). Returns the persisted [AiSettings].
 */
@Inject
public class SaveIntegrationSettingsUseCase(
    private val settingsRepository: UserSettingsRepository,
) {
    public suspend operator fun invoke(draft: IntegrationSettingsDraft): AiSettings {
        val effectiveTtsApiKey =
            if (ttsInheritsAiKey(draft.aiProvider, draft.ttsProvider, draft.ttsSeparateKey)) {
                draft.aiApiKey
            } else {
                draft.ttsApiKey
            }
        val snapshot =
            settingsRepository.updateAiSettings { current ->
                current.copy(
                    aiApiKey = draft.aiApiKey.orNullIfBlank(),
                    aiProvider = draft.aiProvider,
                    aiModel = draft.aiModel.orNullIfBlank(),
                    ttsApiKey = effectiveTtsApiKey.orNullIfBlank(),
                    ttsProvider = draft.ttsProvider,
                    ttsModel = draft.ttsModel.orNullIfBlank(),
                    ttsVoiceId = draft.ttsVoiceId.orNullIfBlank(),
                    ttsSeparateKey = draft.ttsSeparateKey,
                )
            }
        return snapshot.ai
    }
}

private fun String.orNullIfBlank(): String? = trim().takeIf { it.isNotBlank() }
