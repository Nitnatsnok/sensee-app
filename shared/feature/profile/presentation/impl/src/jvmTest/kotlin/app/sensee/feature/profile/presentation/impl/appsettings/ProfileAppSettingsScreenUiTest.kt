package app.sensee.feature.profile.presentation.impl.appsettings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.profile.presentation.api.ProfileAppSettingsAction
import app.sensee.feature.profile.presentation.api.ProfileAppSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileAppSettingsUiState
import app.sensee.ui.designSystem.theme.SenseeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ProfileAppSettingsScreenUiTest {
    @Test
    fun `haptic feedback row toggles when label is clicked`() =
        runComposeUiTest {
            val component = FakeProfileAppSettingsComponent()
            setContent {
                SenseeTheme {
                    ProfileAppSettingsScreen(
                        component = component,
                        textProvider = TestTextProvider,
                    )
                }
            }

            onNodeWithText("Haptic feedback").performClick()

            assertEquals(
                listOf<ProfileAppSettingsAction>(ProfileAppSettingsAction.SetHapticFeedbackEnabled(false)),
                component.actions,
            )
        }

    private class FakeProfileAppSettingsComponent : ProfileAppSettingsComponent {
        override val uiState: StateFlow<ProfileAppSettingsUiState> =
            MutableStateFlow(ProfileAppSettingsUiState(hapticFeedbackEnabled = true))
        val actions: MutableList<ProfileAppSettingsAction> = mutableListOf()

        override fun onAction(action: ProfileAppSettingsAction) {
            actions += action
        }
    }

    private companion object {
        val TestTextProvider: TextProvider =
            MapTextProvider(
                mapOf(
                    ProfileAppSettingsTextKeys.SectionAppearance to "Appearance",
                    ProfileAppSettingsTextKeys.ThemeMode to "Theme",
                    ProfileAppSettingsTextKeys.ThemeModeSystem to "System",
                    ProfileAppSettingsTextKeys.ThemeModeLight to "Light",
                    ProfileAppSettingsTextKeys.ThemeModeDark to "Dark",
                    ProfileAppSettingsTextKeys.SectionInteraction to "Interaction",
                    ProfileAppSettingsTextKeys.HapticFeedback to "Haptic feedback",
                ),
            )
    }
}
