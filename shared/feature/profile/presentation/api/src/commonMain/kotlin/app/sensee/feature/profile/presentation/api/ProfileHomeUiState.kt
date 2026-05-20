package app.sensee.feature.profile.presentation.api

import app.sensee.core.presentation.DataLoadingState
import app.sensee.settings.domain.AiProvider
import app.sensee.settings.domain.TtsProvider
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * AI/TTS settings form state. The form is fully MVI: `draftSnapshot` is the
 * current user-edited values, `savedSnapshot` is the last persisted values, and
 * `isDirty` is structural equality between the two. The screen renders from
 * `draftSnapshot` and sends typed actions on every change (see
 * [ProfileHomeAction]); it does not own draft values.
 *
 * Verification state ([aiKeyCheck] / [ttsKeyCheck]) carries the key/provider it
 * was tagged against ([aiKeyCheckedAgainst] / [ttsKeyCheckedAgainst]); the UI
 * compares those tags to the current draft to decide whether to render Valid /
 * Invalid or fall back to Idle when the user has edited the key since.
 *
 * On a successful verification [availableAiModels] / [availableTtsModels] /
 * [availableTtsVoices] are populated; on failure the screen falls back to manual
 * entry (ADR-005). TTS shares the AI key when both providers are OpenAI and the
 * user has not opted into a separate key — UI composes the effective TTS lists
 * from the AI verify result in that case.
 */
public data class ProfileHomeUiState(
    val loadingState: DataLoadingState = DataLoadingState.Idle,
    val draftSnapshot: ProfileSettingsSnapshot = ProfileSettingsSnapshot(),
    val savedSnapshot: ProfileSettingsSnapshot = ProfileSettingsSnapshot(),
    val aiKeyCheck: KeyCheckStatus = KeyCheckStatus.Idle,
    val aiKeyCheckedAgainst: AiVerifyTarget? = null,
    val availableAiModels: PersistentList<String> = persistentListOf(),
    val ttsKeyCheck: KeyCheckStatus = KeyCheckStatus.Idle,
    val ttsKeyCheckedAgainst: TtsVerifyTarget? = null,
    val availableTtsModels: PersistentList<String> = persistentListOf(),
    val availableTtsVoices: PersistentList<String> = persistentListOf(),
    val isSaved: Boolean = false,
) {
    public val providers: PersistentList<AiProvider> get() = ProfileAiProviders

    public val ttsProviders: PersistentList<TtsProvider> get() = ProfileTtsProviders

    public val isDirty: Boolean get() = draftSnapshot != savedSnapshot
}

private val ProfileAiProviders: PersistentList<AiProvider> = AiProvider.entries.toPersistentList()
private val ProfileTtsProviders: PersistentList<TtsProvider> = TtsProvider.entries.toPersistentList()
