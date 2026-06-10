package app.sensee.feature.profile.presentation.impl.aisettings

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
import app.sensee.ai.core.contract.AiKeyCheck
import app.sensee.ai.core.contract.AiModelCatalog
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.TextProvider
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.feature.profile.presentation.api.AiVerifyTarget
import app.sensee.feature.profile.presentation.api.KeyCheckStatus
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsAction
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsSnapshot
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsUiState
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ProfileAiSettingsScreenUiTest {
    @Test
    fun `editing ai api key hides stale verified state`() =
        runComposeUiTest {
            val tp = TestTextProvider
            val component =
                FakeProfileAiSettingsComponent(
                    ProfileAiSettingsUiState(
                        loadingState = DataLoadingState.Success,
                        draftSnapshot =
                            ProfileAiSettingsSnapshot(
                                aiApiKey = "sk-old",
                                aiProvider = AiProvider.OpenAi,
                            ),
                        savedSnapshot =
                            ProfileAiSettingsSnapshot(
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
                    ProfileAiSettingsScreen(component = component, textProvider = tp)
                }
            }

            onNodeWithText(tp.text(ProfileAiSettingsTextKeys.KeyValid)).assertIsDisplayed()
            onNodeWithText(tp.text(ProfileAiSettingsTextKeys.AiModel)).assertIsDisplayed()

            onNodeWithContentDescription(tp.text(ProfileAiSettingsTextKeys.AiApiKey))
                .performTextReplacement("sk-new")

            // Reflect the action in component state (mirrors what Logic would do).
            component.update {
                it.copy(draftSnapshot = it.draftSnapshot.copy(aiApiKey = "sk-new"))
            }
            waitForIdle()

            // Stale Valid + model picker disappear because the verified target
            // no longer matches the draft (effectiveAiKeyCheck → Idle).
            assertNotDisplayed(tp.text(ProfileAiSettingsTextKeys.KeyValid))
            assertNotDisplayed(tp.text(ProfileAiSettingsTextKeys.AiModel))
        }

    @Test
    fun `changing only the ai provider flips Saved back to Save`() =
        runComposeUiTest {
            val tp = TestTextProvider
            val initialSnapshot =
                ProfileAiSettingsSnapshot(
                    aiApiKey = "sk-x",
                    aiProvider = AiProvider.OpenAi,
                )
            val component =
                FakeProfileAiSettingsComponent(
                    ProfileAiSettingsUiState(
                        loadingState = DataLoadingState.Success,
                        draftSnapshot = initialSnapshot,
                        savedSnapshot = initialSnapshot,
                        isSaved = true,
                    ),
                )
            setContent {
                SenseeTheme {
                    ProfileAiSettingsScreen(component = component, textProvider = tp)
                }
            }

            onNodeWithText(tp.text(ProfileAiSettingsTextKeys.Saved)).assertIsDisplayed()

            // Simulate Logic effect of switching to OpenRouter (in real flow
            // it's dispatched via SetAiProvider action from the SelectField).
            component.update {
                it.copy(
                    draftSnapshot = it.draftSnapshot.copy(aiProvider = AiProvider.OpenRouter, aiModel = ""),
                    isSaved = false,
                )
            }
            waitForIdle()

            onNodeWithText(tp.text(ProfileAiSettingsTextKeys.Save)).assertIsDisplayed()
            assertNotDisplayed(tp.text(ProfileAiSettingsTextKeys.Saved))
        }

    @Test
    fun `happy path - enter key verify save shows Saved and persists settings`() =
        runComposeUiTest {
            val tp = TestTextProvider
            val settings = TestUserSettingsRepository()
            val catalog = TestModelCatalog(AiKeyCheck.Valid(listOf("gpt-4o")))
            val logic = buildLogic(settings, catalog)
            val component = LiveProfileAiSettingsComponent(logic)

            setContent {
                SenseeTheme {
                    ProfileAiSettingsScreen(component = component, textProvider = tp)
                }
            }

            assertNotDisplayed(tp.text(ProfileAiSettingsTextKeys.KeyValid))
            assertNotDisplayed(tp.text(ProfileAiSettingsTextKeys.AiModel))

            onNodeWithContentDescription(tp.text(ProfileAiSettingsTextKeys.AiApiKey))
                .performTextInput("sk-good")
            waitForIdle()

            // Default TtsProvider is ElevenLabs, so the TTS block is in
            // separate-mode and shows its own Verify button — disambiguate by
            // taking the first (AI) one.
            onAllNodesWithText(tp.text(ProfileAiSettingsTextKeys.Verify)).onFirst().performClick()
            waitUntil(timeoutMillis = 3_000) {
                onAllNodesWithText(tp.text(ProfileAiSettingsTextKeys.KeyValid)).fetchSemanticsNodes().isNotEmpty()
            }

            onNodeWithText(tp.text(ProfileAiSettingsTextKeys.AiModel)).assertIsDisplayed()

            scrollToAndClick(hasText(tp.text(ProfileAiSettingsTextKeys.Save)))
            waitUntil(timeoutMillis = 3_000) {
                onAllNodesWithText(tp.text(ProfileAiSettingsTextKeys.Saved)).fetchSemanticsNodes().isNotEmpty()
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
            val component = LiveProfileAiSettingsComponent(logic)

            setContent {
                SenseeTheme {
                    ProfileAiSettingsScreen(component = component, textProvider = tp)
                }
            }

            // Inherit kicks in only when both providers are OpenAI. AI side is
            // OpenAI by default; TTS we set explicitly via the action (the
            // SelectField popup is non-trivial to drive in a headless test).
            component.onAction(ProfileAiSettingsAction.SetTtsProvider(TtsProvider.OpenAi))
            waitForIdle()

            onNodeWithContentDescription(tp.text(ProfileAiSettingsTextKeys.AiApiKey))
                .performTextInput("sk-shared")
            waitForIdle()
            // In inherit-mode the TTS block has no Verify button — there's
            // exactly one Verify (AI) — but call .onFirst() for symmetry with
            // the happy-path test and robustness if the TTS block grows.
            onAllNodesWithText(tp.text(ProfileAiSettingsTextKeys.Verify)).onFirst().performClick()
            waitUntil(timeoutMillis = 3_000) {
                onAllNodesWithText(tp.text(ProfileAiSettingsTextKeys.KeyValid)).fetchSemanticsNodes().isNotEmpty()
            }
            scrollToAndClick(hasText(tp.text(ProfileAiSettingsTextKeys.Save)))
            waitUntil(timeoutMillis = 3_000) {
                onAllNodesWithText(tp.text(ProfileAiSettingsTextKeys.Saved)).fetchSemanticsNodes().isNotEmpty()
            }

            // Disk now has ttsApiKey == aiApiKey (the use case persists the AI
            // key as effective TTS key); form-side draft must stay empty per
            // toProfileSnapshot inherit-normalization.
            assertEquals("sk-shared", settings.snapshot.ai.ttsApiKey)
            assertEquals("", logic.uiState.value.draftSnapshot.ttsApiKey)

            // Toggle into separate-key mode. The TTS API key field becomes
            // visible — and must be empty, not pre-filled with the AI key.
            scrollToAndClick(hasText(tp.text(ProfileAiSettingsTextKeys.TtsUseSeparateKey)))
            waitForIdle()

            onNode(hasScrollToNodeAction())
                .performScrollToNode(hasContentDescription(tp.text(ProfileAiSettingsTextKeys.TtsApiKey)))
            onNodeWithContentDescription(tp.text(ProfileAiSettingsTextKeys.TtsApiKey)).assertIsDisplayed()
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

    private class FakeProfileAiSettingsComponent(
        initialState: ProfileAiSettingsUiState = ProfileAiSettingsUiState(loadingState = DataLoadingState.Success),
    ) : ProfileAiSettingsComponent {
        val dispatched: MutableList<ProfileAiSettingsAction> = mutableListOf()

        override val uiState: StateFlow<ProfileAiSettingsUiState>
            field = MutableStateFlow(initialState)

        override fun onAction(action: ProfileAiSettingsAction) {
            dispatched += action
        }

        fun update(transform: (ProfileAiSettingsUiState) -> ProfileAiSettingsUiState) = uiState.update(transform)
    }

    private class LiveProfileAiSettingsComponent(
        private val logic: ProfileAiSettingsLogic,
    ) : ProfileAiSettingsComponent {
        override val uiState: StateFlow<ProfileAiSettingsUiState> = logic.uiState

        override fun onAction(action: ProfileAiSettingsAction) = logic.onAction(action)
    }

    private fun buildLogic(
        settings: UserSettingsRepository,
        modelCatalog: AiModelCatalog,
        ttsCatalog: TtsCatalog = TestTtsCatalog(),
    ): ProfileAiSettingsLogic =
        ProfileAiSettingsLogic(
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

    /** Stable English copy of [DefaultProfileAiSettingsTextProvider] for assertion stability. */
    private companion object {
        val TestTextProvider: TextProvider =
            MapTextProvider(
                mapOf(
                    ProfileAiSettingsTextKeys.SectionAi to "AI",
                    ProfileAiSettingsTextKeys.AiHint to "AI hint",
                    ProfileAiSettingsTextKeys.AiProvider to "AI provider",
                    ProfileAiSettingsTextKeys.AiApiKey to "AI API key",
                    ProfileAiSettingsTextKeys.AiModel to "AI model",
                    ProfileAiSettingsTextKeys.SectionTts to "TTS",
                    ProfileAiSettingsTextKeys.TtsProvider to "TTS provider",
                    ProfileAiSettingsTextKeys.TtsUsesAiKey to "Uses AI key",
                    ProfileAiSettingsTextKeys.TtsUseSeparateKey to "Use separate key",
                    ProfileAiSettingsTextKeys.TtsApiKey to "TTS API key",
                    ProfileAiSettingsTextKeys.TtsUseAiKey to "Use AI key",
                    ProfileAiSettingsTextKeys.TtsModel to "TTS model",
                    ProfileAiSettingsTextKeys.TtsVoice to "TTS voice",
                    ProfileAiSettingsTextKeys.Save to "Save",
                    ProfileAiSettingsTextKeys.Saved to "Saved",
                    ProfileAiSettingsTextKeys.Verify to "Verify",
                    ProfileAiSettingsTextKeys.Verifying to "Verifying",
                    ProfileAiSettingsTextKeys.KeyValid to "Key valid",
                    ProfileAiSettingsTextKeys.KeyInvalid to "Key invalid: {0}",
                ),
            )
    }
}
