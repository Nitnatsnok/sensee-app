package app.sensee.feature.profile.presentation.api

import app.sensee.settings.domain.AiProvider
import app.sensee.settings.domain.TtsProvider

public sealed interface ProfileAiSettingsAction {
    public sealed interface Draft : ProfileAiSettingsAction

    /** Push a keystroke into [ProfileAiSettingsUiState.draftSnapshot]. */
    public data class SetAiApiKey(
        val value: String,
    ) : Draft

    /** Switch AI provider; verify-context and the AI model in draft are reset. */
    public data class SetAiProvider(
        val provider: AiProvider,
    ) : Draft

    public data class SetAiModel(
        val value: String,
    ) : Draft

    public data class SetTtsApiKey(
        val value: String,
    ) : Draft

    /** Switch TTS provider; verify-context and TTS model/voice in draft are reset. */
    public data class SetTtsProvider(
        val provider: TtsProvider,
    ) : Draft

    public data class SetTtsModel(
        val value: String,
    ) : Draft

    public data class SetTtsVoiceId(
        val value: String,
    ) : Draft

    /** Opt in/out of a separate TTS key while it could be inherited from AI. */
    public data class SetTtsSeparateKey(
        val value: Boolean,
    ) : Draft

    /** Verify the AI key currently in the draft against the chosen provider. */
    public data object VerifyAiKey : ProfileAiSettingsAction

    /** Verify the TTS key currently in the draft against the chosen provider. */
    public data object VerifyTtsKey : ProfileAiSettingsAction

    /** Persist the current draft snapshot. */
    public data object Save : ProfileAiSettingsAction
}
