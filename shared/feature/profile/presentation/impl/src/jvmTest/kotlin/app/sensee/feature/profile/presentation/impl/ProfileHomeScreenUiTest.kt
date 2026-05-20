package app.sensee.feature.profile.presentation.impl

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import app.sensee.ai.core.AiKeyCheck
import app.sensee.ai.core.AiModelCatalog
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.TextProvider
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.feature.profile.presentation.api.AiVerifyTarget
import app.sensee.feature.profile.presentation.api.KeyCheckStatus
import app.sensee.feature.profile.presentation.api.ProfileHomeAction
import app.sensee.feature.profile.presentation.api.ProfileHomeComponent
import app.sensee.feature.profile.presentation.api.ProfileHomeUiState
import app.sensee.feature.profile.presentation.api.ProfileSettingsSnapshot
import app.sensee.settings.domain.AiProvider
import app.sensee.settings.domain.SaveIntegrationSettingsUseCase
import app.sensee.settings.domain.TtsProvider
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import app.sensee.tts.core.TtsCatalog
import app.sensee.tts.core.TtsKeyCheck
import app.sensee.tts.core.TtsKeyVerificationRequest
import app.sensee.ui.designSystem.theme.SenseeTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ProfileHomeScreenUiTest {
    @Test
    fun `editing ai api key hides stale verified state`() =
        runComposeUiTest {
            val tp = TestTextProvider
            val component =
                FakeProfileHomeComponent(
                    ProfileHomeUiState(
                        loadingState = DataLoadingState.Success,
                        draftSnapshot =
                            ProfileSettingsSnapshot(
                                aiApiKey = "sk-old",
                                aiProvider = AiProvider.OpenAi,
                            ),
                        savedSnapshot =
                            ProfileSettingsSnapshot(
                                aiApiKey = "sk-old",
                                aiProvider = AiProvider.OpenAi,
                            ),
                        aiKeyCheck = KeyCheckStatus.Valid,
                        aiKeyCheckedAgainst = AiVerifyTarget("sk-old", AiProvider.OpenAi),
                        availableAiModels = persistentListOf("gpt-4o"),
                    ),
                )

            setContent {
                SenseeTheme {
                    ProfileHomeScreen(component = component, textProvider = tp)
                }
            }

            onNodeWithText(tp.text(ProfileHomeTextKeys.KeyValid)).assertIsDisplayed()
            onNodeWithText(tp.text(ProfileHomeTextKeys.AiModel)).assertIsDisplayed()

            onNodeWithContentDescription(tp.text(ProfileHomeTextKeys.AiApiKey))
                .performTextReplacement("sk-new")

            // Reflect the action in component state (mirrors what Logic would do).
            component.update {
                it.copy(draftSnapshot = it.draftSnapshot.copy(aiApiKey = "sk-new"))
            }
            waitForIdle()

            // Stale Valid + model picker disappear because the verified target
            // no longer matches the draft (effectiveAiKeyCheck → Idle).
            assertNotDisplayed(tp.text(ProfileHomeTextKeys.KeyValid))
            assertNotDisplayed(tp.text(ProfileHomeTextKeys.AiModel))
        }

    @Test
    fun `changing only the ai provider flips Saved back to Save`() =
        runComposeUiTest {
            val tp = TestTextProvider
            val initialSnapshot =
                ProfileSettingsSnapshot(
                    aiApiKey = "sk-x",
                    aiProvider = AiProvider.OpenAi,
                )
            val component =
                FakeProfileHomeComponent(
                    ProfileHomeUiState(
                        loadingState = DataLoadingState.Success,
                        draftSnapshot = initialSnapshot,
                        savedSnapshot = initialSnapshot,
                        isSaved = true,
                    ),
                )
            setContent {
                SenseeTheme {
                    ProfileHomeScreen(component = component, textProvider = tp)
                }
            }

            onNodeWithText(tp.text(ProfileHomeTextKeys.Saved)).assertIsDisplayed()

            // Simulate Logic effect of switching to OpenRouter (in real flow
            // it's dispatched via SetAiProvider action from the SelectField).
            component.update {
                it.copy(
                    draftSnapshot = it.draftSnapshot.copy(aiProvider = AiProvider.OpenRouter, aiModel = ""),
                    isSaved = false,
                )
            }
            waitForIdle()

            onNodeWithText(tp.text(ProfileHomeTextKeys.Save)).assertIsDisplayed()
            assertNotDisplayed(tp.text(ProfileHomeTextKeys.Saved))
        }

    @Test
    fun `happy path - enter key verify save shows Saved and persists settings`() =
        runComposeUiTest {
            val tp = TestTextProvider
            val settings = TestUserSettingsRepository()
            val catalog = TestModelCatalog(AiKeyCheck.Valid(listOf("gpt-4o")))
            val logic = buildLogic(settings, catalog)
            val component = LiveProfileHomeComponent(logic)

            setContent {
                SenseeTheme {
                    ProfileHomeScreen(component = component, textProvider = tp)
                }
            }

            assertNotDisplayed(tp.text(ProfileHomeTextKeys.KeyValid))
            assertNotDisplayed(tp.text(ProfileHomeTextKeys.AiModel))

            onNodeWithContentDescription(tp.text(ProfileHomeTextKeys.AiApiKey))
                .performTextInput("sk-good")
            waitForIdle()

            // Default TtsProvider is ElevenLabs, so the TTS block is in
            // separate-mode and shows its own Verify button — disambiguate by
            // taking the first (AI) one.
            onAllNodesWithText(tp.text(ProfileHomeTextKeys.Verify)).onFirst().performClick()
            waitUntil(timeoutMillis = 3_000) {
                onAllNodesWithText(tp.text(ProfileHomeTextKeys.KeyValid)).fetchSemanticsNodes().isNotEmpty()
            }

            onNodeWithText(tp.text(ProfileHomeTextKeys.AiModel)).assertIsDisplayed()

            scrollToAndClick(hasText(tp.text(ProfileHomeTextKeys.Save)))
            waitUntil(timeoutMillis = 3_000) {
                onAllNodesWithText(tp.text(ProfileHomeTextKeys.Saved)).fetchSemanticsNodes().isNotEmpty()
            }

            assertEquals("sk-good", settings.snapshot.ai.aiApiKey)
        }

    @Test
    fun `inherit toggle after save does not surface the AI key in the TTS field`() =
        runComposeUiTest {
            val tp = TestTextProvider
            val settings = TestUserSettingsRepository()
            val catalog = TestModelCatalog(AiKeyCheck.Valid(listOf("gpt-4o")))
            val logic = buildLogic(settings, catalog)
            val component = LiveProfileHomeComponent(logic)

            setContent {
                SenseeTheme {
                    ProfileHomeScreen(component = component, textProvider = tp)
                }
            }

            // Inherit kicks in only when both providers are OpenAI. AI side is
            // OpenAI by default; TTS we set explicitly via the action (the
            // SelectField popup is non-trivial to drive in a headless test).
            component.onAction(ProfileHomeAction.SetTtsProvider(TtsProvider.OpenAi))
            waitForIdle()

            onNodeWithContentDescription(tp.text(ProfileHomeTextKeys.AiApiKey))
                .performTextInput("sk-shared")
            waitForIdle()
            // In inherit-mode the TTS block has no Verify button — there's
            // exactly one Verify (AI) — but call .onFirst() for symmetry with
            // the happy-path test and robustness if the TTS block grows.
            onAllNodesWithText(tp.text(ProfileHomeTextKeys.Verify)).onFirst().performClick()
            waitUntil(timeoutMillis = 3_000) {
                onAllNodesWithText(tp.text(ProfileHomeTextKeys.KeyValid)).fetchSemanticsNodes().isNotEmpty()
            }
            scrollToAndClick(hasText(tp.text(ProfileHomeTextKeys.Save)))
            waitUntil(timeoutMillis = 3_000) {
                onAllNodesWithText(tp.text(ProfileHomeTextKeys.Saved)).fetchSemanticsNodes().isNotEmpty()
            }

            // Disk now has ttsApiKey == aiApiKey (the use case persists the AI
            // key as effective TTS key); form-side draft must stay empty per
            // toProfileSnapshot inherit-normalization.
            assertEquals("sk-shared", settings.snapshot.ai.ttsApiKey)
            assertEquals("", logic.uiState.value.draftSnapshot.ttsApiKey)

            // Toggle into separate-key mode. The TTS API key field becomes
            // visible — and must be empty, not pre-filled with the AI key.
            scrollToAndClick(hasText(tp.text(ProfileHomeTextKeys.TtsUseSeparateKey)))
            waitForIdle()

            onNode(hasScrollToNodeAction())
                .performScrollToNode(hasContentDescription(tp.text(ProfileHomeTextKeys.TtsApiKey)))
            onNodeWithContentDescription(tp.text(ProfileHomeTextKeys.TtsApiKey)).assertIsDisplayed()
            assertEquals("", logic.uiState.value.draftSnapshot.ttsApiKey)
        }

    // --- helpers ---

    private fun ComposeUiTest.assertNotDisplayed(text: String) {
        val nodes = onAllNodesWithText(text).fetchSemanticsNodes()
        assertTrue(nodes.isEmpty(), "expected no node with text='$text', got ${nodes.size}")
    }

    // LazyColumn lazily realises items, so a node outside the viewport is not
    // in the semantics tree and a direct onNode(...).performClick() fails. The
    // canonical CMP pattern is to scroll the parent (the one carrying the
    // ScrollToIndex action — i.e. the LazyColumn) until the target matcher
    // matches, then act on the now-realised node.
    private fun ComposeUiTest.scrollToAndClick(matcher: SemanticsMatcher) {
        onNode(hasScrollToNodeAction()).performScrollToNode(matcher)
        onNode(matcher).performClick()
    }

    private class FakeProfileHomeComponent(
        initialState: ProfileHomeUiState = ProfileHomeUiState(loadingState = DataLoadingState.Success),
    ) : ProfileHomeComponent {
        private val state = MutableStateFlow(initialState)
        val dispatched: MutableList<ProfileHomeAction> = mutableListOf()

        override val uiState: StateFlow<ProfileHomeUiState> = state.asStateFlow()

        override fun onAction(action: ProfileHomeAction) {
            dispatched += action
        }

        fun update(transform: (ProfileHomeUiState) -> ProfileHomeUiState) = state.update(transform)
    }

    private class LiveProfileHomeComponent(
        private val logic: ProfileHomeLogic,
    ) : ProfileHomeComponent {
        override val uiState: StateFlow<ProfileHomeUiState> = logic.uiState

        override fun onAction(action: ProfileHomeAction) = logic.onAction(action)
    }

    private fun buildLogic(
        settings: UserSettingsRepository,
        modelCatalog: AiModelCatalog,
        ttsCatalog: TtsCatalog = TestTtsCatalog(),
    ): ProfileHomeLogic =
        ProfileHomeLogic(
            settingsRepository = settings,
            saveIntegrationSettings = SaveIntegrationSettingsUseCase(settings),
            modelCatalog = modelCatalog,
            ttsCatalog = ttsCatalog,
            appDispatchers = immediateAppDispatchers(),
            appDiagnostics = noOpAppDiagnostics(),
        )

    private class TestUserSettingsRepository(
        var snapshot: UserSettingsSnapshot = UserSettingsSnapshot(),
    ) : UserSettingsRepository {
        override fun observeSettings(scope: UserSettingsScope): Flow<UserSettingsSnapshot> = flowOf(snapshot)

        override suspend fun readSettings(scope: UserSettingsScope): UserSettingsSnapshot = snapshot

        override suspend fun updateSettings(
            scope: UserSettingsScope,
            transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
        ): UserSettingsSnapshot = transform(snapshot).also { snapshot = it }
    }

    private class TestModelCatalog(
        private val result: AiKeyCheck,
    ) : AiModelCatalog {
        override suspend fun verifyKey(
            baseUrl: String,
            apiKey: String,
        ): AiKeyCheck = result
    }

    private class TestTtsCatalog(
        private val result: TtsKeyCheck = TtsKeyCheck.Valid(emptyList(), emptyList()),
    ) : TtsCatalog {
        override suspend fun verifyKey(request: TtsKeyVerificationRequest): TtsKeyCheck = result
    }

    /** Stable English copy of [DefaultProfileHomeTextProvider] for assertion stability. */
    private companion object {
        val TestTextProvider: TextProvider =
            MapTextProvider(
                mapOf(
                    ProfileHomeTextKeys.SectionAi to "AI",
                    ProfileHomeTextKeys.AiHint to "AI hint",
                    ProfileHomeTextKeys.AiProvider to "AI provider",
                    ProfileHomeTextKeys.AiApiKey to "AI API key",
                    ProfileHomeTextKeys.AiModel to "AI model",
                    ProfileHomeTextKeys.SectionTts to "TTS",
                    ProfileHomeTextKeys.TtsProvider to "TTS provider",
                    ProfileHomeTextKeys.TtsUsesAiKey to "Uses AI key",
                    ProfileHomeTextKeys.TtsUseSeparateKey to "Use separate key",
                    ProfileHomeTextKeys.TtsApiKey to "TTS API key",
                    ProfileHomeTextKeys.TtsUseAiKey to "Use AI key",
                    ProfileHomeTextKeys.TtsModel to "TTS model",
                    ProfileHomeTextKeys.TtsVoice to "TTS voice",
                    ProfileHomeTextKeys.Save to "Save",
                    ProfileHomeTextKeys.Saved to "Saved",
                    ProfileHomeTextKeys.Verify to "Verify",
                    ProfileHomeTextKeys.Verifying to "Verifying",
                    ProfileHomeTextKeys.KeyValid to "Key valid",
                    ProfileHomeTextKeys.KeyInvalid to "Key invalid: {0}",
                ),
            )
    }
}
