package app.sensee.settings.domain

/**
 * User-supplied third-party credentials and provider choice. The [aiApiKey]
 * and [ttsApiKey] fields are populated from the platform secret vault
 * (Keystore/Keychain/OS vault); the rest of the fields ride in the regular
 * SQLite settings table. A blank/null key means the integration is not
 * configured — the seam degrades to its offline default. The AI provider is
 * an [AiProvider] selection (it predefines the base URL); [aiModel] is chosen
 * from the provider's model list, null meaning the provider default.
 */
public data class AiSettings(
    val aiApiKey: String? = null,
    val aiProvider: AiProvider = AiProvider.Default,
    val aiModel: String? = null,
    val ttsApiKey: String? = null,
    val ttsProvider: TtsProvider = TtsProvider.Default,
    val ttsModel: String? = null,
    val ttsVoiceId: String? = null,
    val ttsSeparateKey: Boolean = false,
)

/**
 * The OpenAI TTS key is OpenAI-compatible: when both providers are OpenAI the
 * single AI key covers TTS, unless the user opted into a separate TTS key.
 * Single source for this rule — UI disclosure, verification mirroring, and the
 * save-time effective-key choice all derive from here.
 */
public fun ttsInheritsAiKey(
    aiProvider: AiProvider,
    ttsProvider: TtsProvider,
    ttsSeparateKey: Boolean,
): Boolean =
    aiProvider == AiProvider.OpenAi &&
        ttsProvider == TtsProvider.OpenAi &&
        !ttsSeparateKey
