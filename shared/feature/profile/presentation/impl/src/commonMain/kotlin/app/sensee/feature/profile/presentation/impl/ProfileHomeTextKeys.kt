package app.sensee.feature.profile.presentation.impl

import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.TextKey

internal object ProfileHomeTextKeys {
    val SectionAi = TextKey("profile.home.section.ai")
    val AiHint = TextKey("profile.home.ai_hint")
    val AiProvider = TextKey("profile.home.ai_provider")
    val AiApiKey = TextKey("profile.home.ai_api_key")
    val AiModel = TextKey("profile.home.ai_model")
    val SectionTts = TextKey("profile.home.section.tts")
    val TtsProvider = TextKey("profile.home.tts_provider")
    val TtsUsesAiKey = TextKey("profile.home.tts_uses_ai_key")
    val TtsUseSeparateKey = TextKey("profile.home.tts_use_separate_key")
    val TtsApiKey = TextKey("profile.home.tts_api_key")
    val TtsUseAiKey = TextKey("profile.home.tts_use_ai_key")
    val TtsModel = TextKey("profile.home.tts_model")
    val TtsVoice = TextKey("profile.home.tts_voice")
    val Save = TextKey("profile.home.save")
    val Saved = TextKey("profile.home.saved")
    val Verify = TextKey("profile.home.verify")
    val Verifying = TextKey("profile.home.verifying")
    val KeyValid = TextKey("profile.home.key_valid")
    val KeyInvalid = TextKey("profile.home.key_invalid")
}

internal val DefaultProfileHomeTextProvider =
    MapTextProvider(
        mapOf(
            ProfileHomeTextKeys.SectionAi to "ИИ",
            ProfileHomeTextKeys.AiHint to
                "Необязательно. Хранится только на этом устройстве. " +
                "Без ключей приложение использует встроенного офлайн-ассистента.",
            ProfileHomeTextKeys.AiProvider to "Провайдер ИИ",
            ProfileHomeTextKeys.AiApiKey to "API-ключ ИИ",
            ProfileHomeTextKeys.AiModel to "Модель ИИ",
            ProfileHomeTextKeys.SectionTts to "Озвучивание",
            ProfileHomeTextKeys.TtsProvider to "Провайдер озвучивания",
            ProfileHomeTextKeys.TtsUsesAiKey to "Использует ключ провайдера ИИ",
            ProfileHomeTextKeys.TtsUseSeparateKey to "Использовать отдельный ключ",
            ProfileHomeTextKeys.TtsApiKey to "API-ключ озвучивания",
            ProfileHomeTextKeys.TtsUseAiKey to "Использовать ключ провайдера ИИ",
            ProfileHomeTextKeys.TtsModel to "Модель озвучивания",
            ProfileHomeTextKeys.TtsVoice to "Голос озвучивания",
            ProfileHomeTextKeys.Save to "Сохранить",
            ProfileHomeTextKeys.Saved to "Сохранено",
            ProfileHomeTextKeys.Verify to "Проверить",
            ProfileHomeTextKeys.Verifying to "Проверка…",
            ProfileHomeTextKeys.KeyValid to "✓ Ключ подтверждён",
            ProfileHomeTextKeys.KeyInvalid to "✗ {0} — можно ввести модель вручную",
        ),
    )
