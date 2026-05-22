package app.sensee.feature.profile.presentation.impl.home

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.profile.presentation.api.ProfileHomeComponent
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import app.sensee.ui.designSystem.theme.SenseeTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ProfileHomeScreenUiTest {
    @Test
    fun `menu shows the settings group with a row per category`() =
        runComposeUiTest {
            val component = FakeProfileHomeComponent()
            setContent {
                SenseeTheme {
                    ProfileHomeScreen(
                        component = component,
                        selectedConfig = null,
                        textProvider = TestTextProvider,
                    )
                }
            }

            onNodeWithText("Settings").assertIsDisplayed()
            onNodeWithText("App").assertIsDisplayed()
        }

    @Test
    fun `selecting a category dispatches its config to the component`() =
        runComposeUiTest {
            val component = FakeProfileHomeComponent()
            setContent {
                SenseeTheme {
                    ProfileHomeScreen(
                        component = component,
                        selectedConfig = null,
                        textProvider = TestTextProvider,
                    )
                }
            }

            onNode(hasScrollToNodeAction()).performScrollToNode(hasText("AI and speech"))
            onNodeWithText("AI and speech").performClick()

            assertEquals(listOf<ProfileConfig>(ProfileConfig.AiSettings), component.selected)
        }

    private class FakeProfileHomeComponent : ProfileHomeComponent {
        val selected: MutableList<ProfileConfig> = mutableListOf()

        override val items: ImmutableList<ProfileConfig> =
            persistentListOf(
                ProfileConfig.AppSettings,
                ProfileConfig.LearningSettings,
                ProfileConfig.PracticeSettings,
                ProfileConfig.AiSettings,
                ProfileConfig.ExperimentalSettings,
            )

        override fun onItemSelected(config: ProfileConfig) {
            selected += config
        }
    }

    private companion object {
        val TestTextProvider: TextProvider =
            MapTextProvider(
                mapOf(
                    ProfileHomeTextKeys.GroupSettings to "Settings",
                    ProfileHomeTextKeys.CategoryApp to "App",
                    ProfileHomeTextKeys.CategoryLearning to "Learning",
                    ProfileHomeTextKeys.CategoryPractice to "Practice",
                    ProfileHomeTextKeys.CategoryAi to "AI and speech",
                    ProfileHomeTextKeys.CategoryExperimental to "Experimental",
                    ProfileHomeTextKeys.PlaceholderDescription to "Coming later",
                    ProfileHomeTextKeys.Back to "Back",
                ),
            )
    }
}
