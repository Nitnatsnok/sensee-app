package app.sensee.feature.profile.presentation.impl.aisettings

import app.sensee.feature.profile.presentation.api.AiVerifyTarget
import app.sensee.feature.profile.presentation.api.KeyCheckStatus
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsUiState
import app.sensee.feature.profile.presentation.api.TtsVerifyTarget
import app.sensee.settings.domain.AiProvider
import app.sensee.settings.domain.TtsProvider
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

// UI-derived projections over [ProfileAiSettingsUiState]. They live next to the
// screen because they encode UI decisions (stale-verify detection,
// inherit-derived TTS lists), not domain state.

/**
 * Effective AI key-check from the user's perspective. If the verified
 * key/provider tag no longer matches the current draft (because the user
 * edited the key or changed provider), the stored `Valid`/`Invalid` is stale
 * and we render as `Idle` until a fresh verify lands.
 */
internal fun ProfileAiSettingsUiState.effectiveAiKeyCheck(): KeyCheckStatus {
    val target = aiKeyCheckedAgainst ?: return KeyCheckStatus.Idle
    return if (target.matches(draftSnapshot.aiApiKey, draftSnapshot.aiProvider)) {
        aiKeyCheck
    } else {
        KeyCheckStatus.Idle
    }
}

internal fun ProfileAiSettingsUiState.effectiveTtsKeyCheck(): KeyCheckStatus {
    if (draftSnapshot.ttsInheritsAiKey) return effectiveAiKeyCheck()
    val target = ttsKeyCheckedAgainst ?: return KeyCheckStatus.Idle
    return if (target.matches(draftSnapshot.ttsApiKey, draftSnapshot.ttsProvider)) {
        ttsKeyCheck
    } else {
        KeyCheckStatus.Idle
    }
}

internal fun ProfileAiSettingsUiState.effectiveAvailableTtsModels(): PersistentList<String> =
    when {
        draftSnapshot.ttsInheritsAiKey && effectiveAiKeyCheck() == KeyCheckStatus.Valid -> OpenAiTtsModels
        effectiveTtsKeyCheck() == KeyCheckStatus.Valid -> availableTtsModels
        else -> persistentListOf()
    }

internal fun ProfileAiSettingsUiState.effectiveAvailableTtsVoices(): PersistentList<String> =
    when {
        draftSnapshot.ttsInheritsAiKey && effectiveAiKeyCheck() == KeyCheckStatus.Valid -> OpenAiTtsVoices
        effectiveTtsKeyCheck() == KeyCheckStatus.Valid -> availableTtsVoices
        else -> persistentListOf()
    }

private fun AiVerifyTarget.matches(
    apiKey: String,
    provider: AiProvider,
): Boolean = this.apiKey == apiKey.trim() && this.provider == provider

private fun TtsVerifyTarget.matches(
    apiKey: String,
    provider: TtsProvider,
): Boolean = this.apiKey == apiKey.trim() && this.provider == provider

internal val OpenAiTtsModels: PersistentList<String> = TtsProvider.OpenAi.knownModels.toPersistentList()
internal val OpenAiTtsVoices: PersistentList<String> = TtsProvider.OpenAi.knownVoices.toPersistentList()
