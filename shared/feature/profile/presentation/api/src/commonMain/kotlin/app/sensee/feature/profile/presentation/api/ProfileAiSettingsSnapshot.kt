package app.sensee.feature.profile.presentation.api

import app.sensee.settings.domain.AiProvider
import app.sensee.settings.domain.TtsProvider
import app.sensee.settings.domain.ttsInheritsAiKey

/**
 * Draft/saved snapshot of every field on the AI/TTS settings form. The screen
 * owns one `draftSnapshot` (current user input) and one `savedSnapshot` (last
 * persisted values); `isDirty` is just structural equality between the two.
 *
 * [ttsInheritsAiKey] is the same rule as the domain function — when both
 * providers are OpenAI and the user has not opted into a separate TTS key, the
 * AI key covers TTS. In that case the form-side `ttsApiKey` is kept empty
 * regardless of what's on disk (the use case stores the AI key as effective
 * ttsApiKey for TTS routing — see B3 in the plan).
 */
public data class ProfileAiSettingsSnapshot(
    val aiApiKey: String = "",
    val aiProvider: AiProvider = AiProvider.Default,
    val aiModel: String = "",
    val ttsApiKey: String = "",
    val ttsProvider: TtsProvider = TtsProvider.Default,
    val ttsModel: String = "",
    val ttsVoiceId: String = "",
    val ttsSeparateKey: Boolean = false,
) {
    public val ttsInheritsAiKey: Boolean
        get() = ttsInheritsAiKey(aiProvider, ttsProvider, ttsSeparateKey)
}

/** What an AI verify request was tagged against — used to detect stale verify state on UI. */
public data class AiVerifyTarget(
    val apiKey: String,
    val provider: AiProvider,
)

/** What a TTS verify request was tagged against — used to detect stale verify state on UI. */
public data class TtsVerifyTarget(
    val apiKey: String,
    val provider: TtsProvider,
)
