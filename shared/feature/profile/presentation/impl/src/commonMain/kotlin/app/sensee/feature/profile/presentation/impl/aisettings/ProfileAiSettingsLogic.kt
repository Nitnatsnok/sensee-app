package app.sensee.feature.profile.presentation.impl.aisettings

import app.sensee.ai.core.contract.AiKeyCheck
import app.sensee.ai.core.contract.AiModelCatalog
import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.decompose.logic.BaseLogic
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.profile.presentation.api.AiVerifyTarget
import app.sensee.feature.profile.presentation.api.KeyCheckStatus
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsAction
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsSnapshot
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsUiState
import app.sensee.feature.profile.presentation.api.TtsVerifyTarget
import app.sensee.settings.domain.AiProvider
import app.sensee.settings.domain.AiSettings
import app.sensee.settings.domain.IntegrationSettingsDraft
import app.sensee.settings.domain.SaveIntegrationSettingsUseCase
import app.sensee.settings.domain.TtsProvider
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.ttsInheritsAiKey
import app.sensee.tts.core.TtsCatalog
import app.sensee.tts.core.TtsKeyCheck
import app.sensee.tts.core.TtsKeyVerificationRequest
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AssistedInject
public class ProfileAiSettingsLogic(
    private val settingsRepository: UserSettingsRepository,
    private val saveIntegrationSettings: SaveIntegrationSettingsUseCase,
    private val modelCatalog: AiModelCatalog,
    private val ttsCatalog: TtsCatalog,
    appDispatchers: AppDispatchers,
    appDiagnostics: AppDiagnostics,
) : BaseLogic(appDispatchers, appDiagnostics) {
    @AssistedFactory
    public fun interface Factory {
        public fun create(): ProfileAiSettingsLogic
    }

    private val mutableUiState = MutableStateFlow(ProfileAiSettingsUiState())

    public val uiState: StateFlow<ProfileAiSettingsUiState> = mutableUiState.asStateFlow()

    // A slow verify must not resurrect a stale Valid/model list after the user
    // changed the provider (or the inherit choice); cancel the in-flight one.
    private var aiVerifyJob: Job? = null
    private var ttsVerifyJob: Job? = null

    init {
        load()
    }

    public fun onAction(action: ProfileAiSettingsAction) {
        when (action) {
            ProfileAiSettingsAction.VerifyAiKey -> verifyAiKey()
            ProfileAiSettingsAction.VerifyTtsKey -> verifyTtsKey()
            ProfileAiSettingsAction.Save -> save()
            is ProfileAiSettingsAction.Draft -> applyDraftAction(action)
        }
    }

    private fun applyDraftAction(action: ProfileAiSettingsAction.Draft) {
        when (action) {
            is ProfileAiSettingsAction.SetAiApiKey -> updateDraft { it.copy(aiApiKey = action.value) }
            is ProfileAiSettingsAction.SetAiModel -> updateDraft { it.copy(aiModel = action.value) }
            is ProfileAiSettingsAction.SetTtsApiKey -> updateDraft { it.copy(ttsApiKey = action.value) }
            is ProfileAiSettingsAction.SetTtsModel -> updateDraft { it.copy(ttsModel = action.value) }
            is ProfileAiSettingsAction.SetTtsVoiceId -> updateDraft { it.copy(ttsVoiceId = action.value) }
            is ProfileAiSettingsAction.SetAiProvider -> applyAiProviderChange(action.provider)
            is ProfileAiSettingsAction.SetTtsProvider -> applyTtsProviderChange(action.provider)
            is ProfileAiSettingsAction.SetTtsSeparateKey -> applyTtsSeparateKeyChange(action.value)
        }
    }

    public fun load() {
        logicScope.launch {
            mutableUiState.update { it.copy(loadingState = DataLoadingState.Loading) }
            runCatchingCancellable { settingsRepository.readSettings().ai }
                .onSuccess { ai ->
                    val snapshot = ai.toProfileSnapshot()
                    mutableUiState.update {
                        ProfileAiSettingsUiState(
                            loadingState = DataLoadingState.Success,
                            draftSnapshot = snapshot,
                            savedSnapshot = snapshot,
                        )
                    }
                    // A stored key auto-verifies on open so the model/voice
                    // pickers are there without a manual click. /v1/models is a
                    // free metadata call (no token/quota cost); offline just
                    // degrades to manual entry with the saved value preserved.
                    if (snapshot.aiApiKey.isNotBlank()) {
                        verifyAiKey()
                    }
                    // Inherited OpenAI TTS is covered by the AI verify above.
                    if (!snapshot.ttsInheritsAiKey && snapshot.ttsApiKey.isNotBlank()) {
                        verifyTtsKey()
                    }
                }.onFailure { throwable ->
                    logger.error(throwable) { "Failed to read integration settings" }
                    mutableUiState.update { it.copy(loadingState = DataLoadingState.Error(throwable)) }
                }
        }
    }

    private fun applyAiProviderChange(provider: AiProvider) {
        // Changing the provider invalidates any in-flight or completed check
        // (the saved Valid is for the old base URL/model list); the TTS verify
        // also dies because inherit may flip with the AI provider.
        aiVerifyJob?.cancel()
        ttsVerifyJob?.cancel()
        mutableUiState.update { state ->
            val newDraft =
                state.draftSnapshot
                    .copy(aiProvider = provider, aiModel = "")
                    .normalizedForInherit()
            state.copy(
                draftSnapshot = newDraft,
                aiKeyCheck = KeyCheckStatus.Idle,
                aiKeyCheckedAgainst = null,
                availableAiModels = persistentListOf(),
                ttsKeyCheck = KeyCheckStatus.Idle,
                ttsKeyCheckedAgainst = null,
                availableTtsModels = persistentListOf(),
                availableTtsVoices = persistentListOf(),
                isSaved = false,
            )
        }
    }

    private fun applyTtsProviderChange(provider: TtsProvider) {
        ttsVerifyJob?.cancel()
        mutableUiState.update { state ->
            val newDraft =
                state.draftSnapshot
                    .copy(ttsProvider = provider, ttsModel = "", ttsVoiceId = "")
                    .normalizedForInherit()
            state.copy(
                draftSnapshot = newDraft,
                ttsKeyCheck = KeyCheckStatus.Idle,
                ttsKeyCheckedAgainst = null,
                availableTtsModels = persistentListOf(),
                availableTtsVoices = persistentListOf(),
                isSaved = false,
            )
        }
    }

    private fun applyTtsSeparateKeyChange(value: Boolean) {
        ttsVerifyJob?.cancel()
        mutableUiState.update { state ->
            val newDraft = state.draftSnapshot.copy(ttsSeparateKey = value).normalizedForInherit()
            state.copy(
                draftSnapshot = newDraft,
                ttsKeyCheck = KeyCheckStatus.Idle,
                ttsKeyCheckedAgainst = null,
                availableTtsModels = persistentListOf(),
                availableTtsVoices = persistentListOf(),
                isSaved = false,
            )
        }
    }

    // No runCatchingCancellable: the AI/TTS seam never throws across its
    // boundary by contract (ADR-005), it returns a typed Invalid result.
    public fun verifyAiKey() {
        val draft = mutableUiState.value.draftSnapshot
        val apiKey = draft.aiApiKey.trim()
        if (apiKey.isEmpty()) return
        val provider = draft.aiProvider
        mutableUiState.update {
            it.copy(
                aiKeyCheck = KeyCheckStatus.Checking,
                aiKeyCheckedAgainst = null,
                isSaved = false,
            )
        }
        aiVerifyJob?.cancel()
        aiVerifyJob =
            logicScope.launch {
                val target = AiVerifyTarget(apiKey, provider)
                when (val result = modelCatalog.verifyKey(provider.baseUrl, apiKey)) {
                    is AiKeyCheck.Valid ->
                        mutableUiState.update {
                            it.copy(
                                aiKeyCheck = KeyCheckStatus.Valid,
                                aiKeyCheckedAgainst = target,
                                availableAiModels = result.models.toPersistentList(),
                            )
                        }

                    is AiKeyCheck.Invalid ->
                        mutableUiState.update {
                            it.copy(
                                aiKeyCheck = KeyCheckStatus.Invalid(result.reason),
                                aiKeyCheckedAgainst = target,
                                availableAiModels = persistentListOf(),
                            )
                        }
                }
            }
    }

    public fun verifyTtsKey() {
        val draft = mutableUiState.value.draftSnapshot
        val apiKey = draft.ttsApiKey.trim()
        if (apiKey.isEmpty()) return
        val provider = draft.ttsProvider
        mutableUiState.update {
            it.copy(
                ttsKeyCheck = KeyCheckStatus.Checking,
                ttsKeyCheckedAgainst = null,
                isSaved = false,
            )
        }
        ttsVerifyJob?.cancel()
        ttsVerifyJob =
            logicScope.launch {
                val target = TtsVerifyTarget(apiKey, provider)
                when (provider) {
                    TtsProvider.ElevenLabs -> verifyElevenLabs(apiKey, target)
                    // A separate OpenAI TTS key is OpenAI-compatible: reuse the
                    // AI seam's real check; voices stay the fixed known set.
                    TtsProvider.OpenAi -> verifyOpenAiTts(apiKey, target)
                }
            }
    }

    private suspend fun verifyElevenLabs(
        apiKey: String,
        target: TtsVerifyTarget,
    ) {
        val request = TtsKeyVerificationRequest(providerId = TtsProvider.ElevenLabs.id, apiKey = apiKey)
        when (val result = ttsCatalog.verifyKey(request)) {
            is TtsKeyCheck.Valid ->
                mutableUiState.update {
                    it.copy(
                        ttsKeyCheck = KeyCheckStatus.Valid,
                        ttsKeyCheckedAgainst = target,
                        availableTtsModels = result.models.toPersistentList(),
                        availableTtsVoices = result.voices.toPersistentList(),
                    )
                }

            is TtsKeyCheck.Invalid ->
                mutableUiState.update {
                    it.copy(
                        ttsKeyCheck = KeyCheckStatus.Invalid(result.reason),
                        ttsKeyCheckedAgainst = target,
                        availableTtsModels = persistentListOf(),
                        availableTtsVoices = persistentListOf(),
                    )
                }
        }
    }

    private suspend fun verifyOpenAiTts(
        apiKey: String,
        target: TtsVerifyTarget,
    ) {
        when (val result = modelCatalog.verifyKey(TtsProvider.OpenAi.baseUrl, apiKey)) {
            is AiKeyCheck.Valid ->
                mutableUiState.update {
                    it.copy(
                        ttsKeyCheck = KeyCheckStatus.Valid,
                        ttsKeyCheckedAgainst = target,
                        availableTtsModels = TtsProvider.OpenAi.knownModels.toPersistentList(),
                        availableTtsVoices = TtsProvider.OpenAi.knownVoices.toPersistentList(),
                    )
                }

            is AiKeyCheck.Invalid ->
                mutableUiState.update {
                    it.copy(
                        ttsKeyCheck = KeyCheckStatus.Invalid(result.reason),
                        ttsKeyCheckedAgainst = target,
                        availableTtsModels = persistentListOf(),
                        availableTtsVoices = persistentListOf(),
                    )
                }
        }
    }

    public fun save() {
        // Capture user-intent at click time. A slow save must not be undone by
        // edits the user makes while it's in-flight: if draft moves during
        // save, we keep that draft (and skip the Saved indicator) instead of
        // overwriting with the just-persisted snapshot.
        val initial = mutableUiState.value.draftSnapshot
        logicScope.launch {
            runCatchingCancellable { saveIntegrationSettings(initial.toIntegrationDraft()) }
                .onSuccess { ai ->
                    val newSaved = ai.toProfileSnapshot()
                    mutableUiState.update { state ->
                        val unchangedDuringSave = state.draftSnapshot == initial
                        state.copy(
                            savedSnapshot = newSaved,
                            draftSnapshot = if (unchangedDuringSave) newSaved else state.draftSnapshot,
                            isSaved = unchangedDuringSave,
                        )
                    }
                }.onFailure { throwable ->
                    logger.error(throwable) { "Failed to save integration settings" }
                    crashReporter.recordException(
                        throwable,
                        attributes =
                            mapOf(
                                "area" to "profile",
                                "operation" to "save_integration_settings",
                                "ai_provider" to initial.aiProvider.name,
                                "tts_provider" to initial.ttsProvider.name,
                                "tts_separate_key" to initial.ttsSeparateKey.toString(),
                            ),
                    )
                }
        }
    }

    // Single mutation entrypoint. Two responsibilities:
    //  - normalize inherit-mode invariants centrally (see normalizedForInherit);
    //  - drop no-op writes so the UI binding's upstream echo of a freshly-saved
    //    value (downstream → TextFieldState → snapshotFlow → upstream Set*) is
    //    idempotent for isSaved.
    private inline fun updateDraft(transform: (ProfileAiSettingsSnapshot) -> ProfileAiSettingsSnapshot) {
        mutableUiState.update { state ->
            val newDraft = transform(state.draftSnapshot).normalizedForInherit()
            if (newDraft == state.draftSnapshot) {
                state
            } else {
                state.copy(draftSnapshot = newDraft, isSaved = false)
            }
        }
    }
}

/**
 * In inherit-mode the form-side `ttsApiKey` is always empty regardless of what
 * lives on disk — the use case persists the AI key as the effective ttsApiKey
 * for TTS routing (see [SaveIntegrationSettingsUseCase]), and we don't want
 * that to leak back into the visible TTS field when the user opts out of
 * inheriting later.
 */
private fun ProfileAiSettingsSnapshot.normalizedForInherit(): ProfileAiSettingsSnapshot =
    if (ttsInheritsAiKey && ttsApiKey.isNotEmpty()) copy(ttsApiKey = "") else this

private fun ProfileAiSettingsSnapshot.toIntegrationDraft(): IntegrationSettingsDraft =
    IntegrationSettingsDraft(
        aiApiKey = aiApiKey,
        aiProvider = aiProvider,
        aiModel = aiModel,
        ttsApiKey = ttsApiKey,
        ttsProvider = ttsProvider,
        ttsModel = ttsModel,
        ttsVoiceId = ttsVoiceId,
        ttsSeparateKey = ttsSeparateKey,
    )

private fun AiSettings.toProfileSnapshot(): ProfileAiSettingsSnapshot {
    val inherits = ttsInheritsAiKey(aiProvider, ttsProvider, ttsSeparateKey)
    return ProfileAiSettingsSnapshot(
        aiApiKey = aiApiKey.orEmpty(),
        aiProvider = aiProvider,
        aiModel = aiModel.orEmpty(),
        // See [normalizedForInherit] — same invariant on the load path.
        ttsApiKey = if (inherits) "" else ttsApiKey.orEmpty(),
        ttsProvider = ttsProvider,
        ttsModel = ttsModel.orEmpty(),
        ttsVoiceId = ttsVoiceId.orEmpty(),
        ttsSeparateKey = ttsSeparateKey,
    )
}
