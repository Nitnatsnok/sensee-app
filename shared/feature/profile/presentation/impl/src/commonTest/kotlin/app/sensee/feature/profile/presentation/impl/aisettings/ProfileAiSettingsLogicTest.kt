package app.sensee.feature.profile.presentation.impl.aisettings

import app.sensee.ai.core.contract.AiKeyCheck
import app.sensee.ai.core.contract.AiModelCatalog
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.feature.profile.presentation.api.AiVerifyTarget
import app.sensee.feature.profile.presentation.api.KeyCheckStatus
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsAction
import app.sensee.feature.profile.presentation.api.TtsVerifyTarget
import app.sensee.settings.domain.AiProvider
import app.sensee.settings.domain.SaveIntegrationSettingsUseCase
import app.sensee.settings.domain.TtsProvider
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import app.sensee.tts.core.TtsCatalog
import app.sensee.tts.core.TtsKeyCheck
import app.sensee.tts.core.TtsKeyVerificationRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests dispatch through the public action interface — the per-field setter
 * methods on [ProfileAiSettingsLogic] are private and routed via
 * [ProfileAiSettingsLogic.onAction]. These extensions are typed shortcuts used
 * only by tests.
 */
private fun ProfileAiSettingsLogic.setAiApiKey(value: String) {
    onAction(ProfileAiSettingsAction.SetAiApiKey(value))
}

private fun ProfileAiSettingsLogic.setAiModel(value: String) {
    onAction(ProfileAiSettingsAction.SetAiModel(value))
}

private fun ProfileAiSettingsLogic.setTtsApiKey(value: String) {
    onAction(ProfileAiSettingsAction.SetTtsApiKey(value))
}

private fun ProfileAiSettingsLogic.setTtsModel(value: String) {
    onAction(ProfileAiSettingsAction.SetTtsModel(value))
}

private fun ProfileAiSettingsLogic.setTtsVoiceId(value: String) {
    onAction(ProfileAiSettingsAction.SetTtsVoiceId(value))
}

private fun ProfileAiSettingsLogic.setAiProvider(provider: AiProvider) {
    onAction(ProfileAiSettingsAction.SetAiProvider(provider))
}

private fun ProfileAiSettingsLogic.setTtsProvider(provider: TtsProvider) {
    onAction(ProfileAiSettingsAction.SetTtsProvider(provider))
}

private fun ProfileAiSettingsLogic.setTtsSeparateKey(value: Boolean) {
    onAction(ProfileAiSettingsAction.SetTtsSeparateKey(value))
}

class ProfileAiSettingsLogicTest {
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

    private class FakeModelCatalog(
        var result: AiKeyCheck = AiKeyCheck.Valid(listOf("gpt-4o")),
    ) : AiModelCatalog {
        val checkedKeys = mutableListOf<String>()

        override suspend fun verifyKey(
            baseUrl: String,
            apiKey: String,
        ): AiKeyCheck {
            checkedKeys += apiKey
            return result
        }
    }

    private class FakeTtsCatalog(
        var result: TtsKeyCheck = TtsKeyCheck.Valid(listOf("eleven_multilingual_v2"), listOf("rachel")),
    ) : TtsCatalog {
        override suspend fun verifyKey(request: TtsKeyVerificationRequest): TtsKeyCheck = result
    }

    private fun logic(
        settings: UserSettingsRepository = FakeSettings(),
        modelCatalog: AiModelCatalog = FakeModelCatalog(),
        ttsCatalog: TtsCatalog = FakeTtsCatalog(),
    ) = ProfileAiSettingsLogic(
        settingsRepository = settings,
        saveIntegrationSettings = SaveIntegrationSettingsUseCase(settings),
        modelCatalog = modelCatalog,
        ttsCatalog = ttsCatalog,
        appDispatchers = immediateAppDispatchers(),
        appDiagnostics = noOpAppDiagnostics(),
    )

    @Test
    fun `save persists the chosen provider and maps blank fields to unconfigured`() {
        val settings = FakeSettings()
        val logic = logic(settings)

        logic.setAiApiKey("  sk-abc  ")
        logic.setAiProvider(AiProvider.OpenRouter)
        logic.setAiModel("   ")
        logic.setTtsApiKey("tts-xyz")
        logic.setTtsProvider(TtsProvider.OpenAi)
        logic.setTtsSeparateKey(true)
        logic.setTtsModel("")
        logic.setTtsVoiceId("  alloy  ")
        logic.save()

        val ai = settings.snapshot.ai
        assertEquals("sk-abc", ai.aiApiKey)
        assertEquals(AiProvider.OpenRouter, ai.aiProvider)
        assertNull(ai.aiModel)
        assertEquals("tts-xyz", ai.ttsApiKey)
        assertEquals(TtsProvider.OpenAi, ai.ttsProvider)
        assertNull(ai.ttsModel)
        assertEquals("alloy", ai.ttsVoiceId)
    }

    @Test
    fun `saving inherited OpenAI TTS persists the AI key so TTS routing works`() {
        val settings = FakeSettings()
        val logic = logic(settings)

        // OpenAi/OpenAi/separate=false → inherit; ttsApiKey in draft is empty
        // by normalization, the use case substitutes the AI key. (Default
        // TtsProvider is ElevenLabs, so we set OpenAi explicitly.)
        logic.setAiApiKey("sk-shared")
        logic.setAiModel("gpt-4o")
        logic.setTtsProvider(TtsProvider.OpenAi)
        logic.setTtsModel("tts-1")
        logic.setTtsVoiceId("alloy")
        logic.save()

        assertEquals("sk-shared", settings.snapshot.ai.ttsApiKey)
        // Form-side draft stays empty per inherit-normalization.
        assertEquals("", logic.uiState.value.draftSnapshot.ttsApiKey)
    }

    @Test
    fun `a separate TTS key is persisted as itself and not overwritten by the AI key`() {
        val settings = FakeSettings()
        val logic = logic(settings)
        logic.setTtsSeparateKey(true)
        logic.setAiApiKey("sk-ai")
        logic.setAiModel("gpt-4o")
        logic.setTtsApiKey("sk-tts-separate")
        logic.setTtsModel("tts-1")
        logic.setTtsVoiceId("alloy")
        logic.save()

        assertEquals("sk-tts-separate", settings.snapshot.ai.ttsApiKey)
        assertEquals(true, settings.snapshot.ai.ttsSeparateKey)
    }

    @Test
    fun `loading a stored separate OpenAI TTS key keeps it separate and verifies that key`() {
        val stored =
            UserSettingsSnapshot().let {
                it.copy(
                    ai =
                        it.ai.copy(
                            aiApiKey = "sk-ai",
                            aiProvider = AiProvider.OpenAi,
                            ttsApiKey = "sk-tts-separate",
                            ttsProvider = TtsProvider.OpenAi,
                            ttsSeparateKey = true,
                        ),
                )
            }
        val catalog = FakeModelCatalog(AiKeyCheck.Valid(listOf("gpt-4o")))

        val state = logic(settings = FakeSettings(stored), modelCatalog = catalog).uiState.value

        assertEquals(true, state.draftSnapshot.ttsSeparateKey)
        assertEquals(false, state.draftSnapshot.ttsInheritsAiKey)
        assertEquals("sk-tts-separate", state.draftSnapshot.ttsApiKey)
        assertEquals(KeyCheckStatus.Valid, state.ttsKeyCheck)
        assertEquals(
            TtsVerifyTarget("sk-tts-separate", TtsProvider.OpenAi),
            state.ttsKeyCheckedAgainst,
        )
        assertEquals(listOf("sk-ai", "sk-tts-separate"), catalog.checkedKeys)
    }

    @Test
    fun `a stored key auto-verifies on open so the picker is ready`() {
        val stored =
            UserSettingsSnapshot().let {
                it.copy(ai = it.ai.copy(aiApiKey = "sk-stored", aiProvider = AiProvider.OpenRouter))
            }

        val state =
            logic(
                settings = FakeSettings(stored),
                modelCatalog = FakeModelCatalog(AiKeyCheck.Valid(listOf("gpt-4o"))),
            ).uiState.value

        assertEquals(DataLoadingState.Success, state.loadingState)
        assertEquals("sk-stored", state.draftSnapshot.aiApiKey)
        assertEquals(KeyCheckStatus.Valid, state.aiKeyCheck)
        assertEquals(
            AiVerifyTarget("sk-stored", AiProvider.OpenRouter),
            state.aiKeyCheckedAgainst,
        )
        assertEquals(listOf("gpt-4o"), state.availableAiModels)
    }

    @Test
    fun `no stored key keeps the picker collapsed without a network call`() {
        val state = logic(FakeSettings()).uiState.value

        assertEquals(DataLoadingState.Success, state.loadingState)
        assertEquals(KeyCheckStatus.Idle, state.aiKeyCheck)
        assertEquals(emptyList(), state.availableAiModels)
    }

    @Test
    fun `verifying a valid AI key opens the model list`() {
        val logic =
            logic(modelCatalog = FakeModelCatalog(AiKeyCheck.Valid(listOf("gpt-4o", "o3-mini"))))

        logic.setAiApiKey("sk-good")
        logic.verifyAiKey()

        val state = logic.uiState.value
        assertEquals(KeyCheckStatus.Valid, state.aiKeyCheck)
        assertEquals(listOf("gpt-4o", "o3-mini"), state.availableAiModels)
        assertEquals(
            AiVerifyTarget("sk-good", AiProvider.OpenAi),
            state.aiKeyCheckedAgainst,
        )
    }

    @Test
    fun `verifying an invalid AI key reports the reason and offers no list`() {
        val logic =
            logic(modelCatalog = FakeModelCatalog(AiKeyCheck.Invalid("API key was rejected by the provider")))

        logic.setAiApiKey("sk-bad")
        logic.verifyAiKey()

        val state = logic.uiState.value
        assertEquals(KeyCheckStatus.Invalid("API key was rejected by the provider"), state.aiKeyCheck)
        assertEquals(emptyList(), state.availableAiModels)
    }

    @Test
    fun `selecting a provider resets a previous key check and model`() {
        val logic = logic(modelCatalog = FakeModelCatalog(AiKeyCheck.Valid(listOf("gpt-4o"))))
        logic.setAiApiKey("sk-good")
        logic.verifyAiKey()

        logic.setAiProvider(AiProvider.OpenRouter)

        val state = logic.uiState.value
        assertEquals(AiProvider.OpenRouter, state.draftSnapshot.aiProvider)
        assertEquals(KeyCheckStatus.Idle, state.aiKeyCheck)
        assertNull(state.aiKeyCheckedAgainst)
        assertEquals(emptyList(), state.availableAiModels)
    }

    @Test
    fun `OpenAI TTS inherits the AI key check`() {
        val logic = logic(modelCatalog = FakeModelCatalog(AiKeyCheck.Valid(listOf("gpt-4o"))))
        logic.setTtsProvider(TtsProvider.OpenAi)
        logic.setAiApiKey("sk-good")

        logic.verifyAiKey()

        val state = logic.uiState.value
        assertTrue(state.draftSnapshot.ttsInheritsAiKey)
        assertEquals(KeyCheckStatus.Valid, state.aiKeyCheck)
    }

    @Test
    fun `opting into a separate TTS key stops inheriting the AI key`() {
        val logic = logic()
        logic.setTtsProvider(TtsProvider.OpenAi)
        assertTrue(logic.uiState.value.draftSnapshot.ttsInheritsAiKey)

        logic.setTtsSeparateKey(true)

        assertEquals(false, logic.uiState.value.draftSnapshot.ttsInheritsAiKey)
    }

    @Test
    fun `verifying an ElevenLabs TTS key opens its live models and voices`() {
        val logic =
            logic(
                ttsCatalog =
                    FakeTtsCatalog(
                        TtsKeyCheck.Valid(listOf("eleven_turbo_v2_5"), listOf("rachel", "adam")),
                    ),
            )
        logic.setTtsProvider(TtsProvider.ElevenLabs)
        logic.setTtsApiKey("el-key")

        logic.verifyTtsKey()

        val state = logic.uiState.value
        assertEquals(KeyCheckStatus.Valid, state.ttsKeyCheck)
        assertEquals(listOf("eleven_turbo_v2_5"), state.availableTtsModels)
        assertEquals(listOf("rachel", "adam"), state.availableTtsVoices)
    }

    // --- New tests for snapshot model & edge cases ---

    @Test
    fun `repeated setAiApiKey with the same value preserves isSaved`() {
        // Echo from the UI binding (downstream sync re-pushes saved value into
        // TextFieldState → snapshotFlow re-emits → upstream SetAiApiKey) must
        // not undo a freshly-set isSaved.
        val settings = FakeSettings()
        val logic = logic(settings)
        logic.setAiApiKey("sk-good")
        logic.save()
        assertTrue(logic.uiState.value.isSaved)

        logic.setAiApiKey(logic.uiState.value.draftSnapshot.aiApiKey)

        assertTrue(logic.uiState.value.isSaved)
    }

    @Test
    fun `save does not overwrite draft if user edited during save`() {
        // Use a settings repo whose updateSettings suspends until we resolve
        // a deferred — this lets us inject an edit while save is in flight.
        val gate = CompletableDeferred<Unit>()
        val settings =
            object : UserSettingsRepository {
                var snapshot = UserSettingsSnapshot()

                override fun observeSettings(scope: UserSettingsScope) = flowOf(snapshot)

                override suspend fun readSettings(scope: UserSettingsScope) = snapshot

                override suspend fun updateSettings(
                    scope: UserSettingsScope,
                    transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
                ): UserSettingsSnapshot {
                    gate.await()
                    snapshot = transform(snapshot)
                    return snapshot
                }
            }
        val logic = logic(settings)

        logic.setAiApiKey("sk-initial")
        logic.setAiModel("model-initial")
        logic.save()
        logic.setAiModel("model-during-save")
        gate.complete(Unit)

        val state = logic.uiState.value
        assertEquals("model-during-save", state.draftSnapshot.aiModel)
        assertFalse(state.isSaved)
    }

    @Test
    fun `inherit toggle clears draft ttsApiKey unconditionally`() {
        // In separate mode the user typed a TTS key. Switching to inherit
        // (OpenAi/OpenAi/separate=false) must clear it from the form
        // regardless of whether it equals aiApiKey.
        val logic = logic()
        logic.setTtsProvider(TtsProvider.OpenAi)
        logic.setTtsSeparateKey(true)
        logic.setTtsApiKey("sk-x-not-equal-to-ai")
        assertEquals("sk-x-not-equal-to-ai", logic.uiState.value.draftSnapshot.ttsApiKey)

        logic.setTtsSeparateKey(false)

        assertTrue(logic.uiState.value.draftSnapshot.ttsInheritsAiKey)
        assertEquals("", logic.uiState.value.draftSnapshot.ttsApiKey)
    }

    @Test
    fun `isDirty becomes true on provider change alone`() {
        val stored =
            UserSettingsSnapshot().let {
                it.copy(ai = it.ai.copy(aiApiKey = "sk-x", aiProvider = AiProvider.OpenAi))
            }
        val logic = logic(settings = FakeSettings(stored))
        // After auto-verify the draft equals saved → isDirty false.
        assertFalse(logic.uiState.value.isDirty)

        logic.setAiProvider(AiProvider.OpenRouter)

        assertTrue(logic.uiState.value.isDirty)
    }

    @Test
    fun `load maps inherit-mode ttsApiKey to empty in draft snapshot`() {
        // What's on disk: aiApiKey == ttsApiKey under inherit (the use case
        // wrote them that way). The form must not surface that AI key in the
        // TTS field once the user opts out of inheriting.
        val stored =
            UserSettingsSnapshot().let {
                it.copy(
                    ai =
                        it.ai.copy(
                            aiApiKey = "sk-shared",
                            ttsApiKey = "sk-shared",
                            aiProvider = AiProvider.OpenAi,
                            ttsProvider = TtsProvider.OpenAi,
                            ttsSeparateKey = false,
                        ),
                )
            }

        val state = logic(settings = FakeSettings(stored)).uiState.value

        assertTrue(state.draftSnapshot.ttsInheritsAiKey)
        assertEquals("", state.draftSnapshot.ttsApiKey)
    }
}
