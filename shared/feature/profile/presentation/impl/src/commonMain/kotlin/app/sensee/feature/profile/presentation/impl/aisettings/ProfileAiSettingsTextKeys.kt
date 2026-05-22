package app.sensee.feature.profile.presentation.impl.aisettings

import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.TextKey

internal object ProfileAiSettingsTextKeys {
    val SectionAi = TextKey("profile.aiSettings.section.ai")
    val AiHint = TextKey("profile.aiSettings.ai_hint")
    val AiProvider = TextKey("profile.aiSettings.ai_provider")
    val AiApiKey = TextKey("profile.aiSettings.ai_api_key")
    val AiModel = TextKey("profile.aiSettings.ai_model")
    val SectionTts = TextKey("profile.aiSettings.section.tts")
    val TtsProvider = TextKey("profile.aiSettings.tts_provider")
    val TtsUsesAiKey = TextKey("profile.aiSettings.tts_uses_ai_key")
    val TtsUseSeparateKey = TextKey("profile.aiSettings.tts_use_separate_key")
    val TtsApiKey = TextKey("profile.aiSettings.tts_api_key")
    val TtsUseAiKey = TextKey("profile.aiSettings.tts_use_ai_key")
    val TtsModel = TextKey("profile.aiSettings.tts_model")
    val TtsVoice = TextKey("profile.aiSettings.tts_voice")
    val Save = TextKey("profile.aiSettings.save")
    val Saved = TextKey("profile.aiSettings.saved")
    val Verify = TextKey("profile.aiSettings.verify")
    val Verifying = TextKey("profile.aiSettings.verifying")
    val KeyValid = TextKey("profile.aiSettings.key_valid")
    val KeyInvalid = TextKey("profile.aiSettings.key_invalid")
}

internal val DefaultProfileAiSettingsTextProvider =
    MapTextProvider(
        mapOf(
            ProfileAiSettingsTextKeys.SectionAi to "ИИ",
            ProfileAiSettingsTextKeys.AiHint to
                "Необязательно. Хранится только на этом устройстве. " +
                "Без ключей приложение использует встроенного офлайн-ассистента.",
            ProfileAiSettingsTextKeys.AiProvider to "Провайдер ИИ",
            ProfileAiSettingsTextKeys.AiApiKey to "API-ключ ИИ",
            ProfileAiSettingsTextKeys.AiModel to "Модель ИИ",
            ProfileAiSettingsTextKeys.SectionTts to "Озвучивание",
            ProfileAiSettingsTextKeys.TtsProvider to "Провайдер озвучивания",
            ProfileAiSettingsTextKeys.TtsUsesAiKey to "Использует ключ провайдера ИИ",
            ProfileAiSettingsTextKeys.TtsUseSeparateKey to "Использовать отдельный ключ",
            ProfileAiSettingsTextKeys.TtsApiKey to "API-ключ озвучивания",
            ProfileAiSettingsTextKeys.TtsUseAiKey to "Использовать ключ провайдера ИИ",
            ProfileAiSettingsTextKeys.TtsModel to "Модель озвучивания",
            ProfileAiSettingsTextKeys.TtsVoice to "Голос озвучивания",
            ProfileAiSettingsTextKeys.Save to "Сохранить",
            ProfileAiSettingsTextKeys.Saved to "Сохранено",
            ProfileAiSettingsTextKeys.Verify to "Проверить",
            ProfileAiSettingsTextKeys.Verifying to "Проверка…",
            ProfileAiSettingsTextKeys.KeyValid to "✓ Ключ подтверждён",
            ProfileAiSettingsTextKeys.KeyInvalid to "✗ {0} — можно ввести модель вручную",
        ),
    )
