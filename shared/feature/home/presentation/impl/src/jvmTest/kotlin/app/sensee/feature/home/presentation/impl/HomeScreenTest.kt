package app.sensee.feature.home.presentation.impl

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.home.presentation.api.HomeAction
import app.sensee.feature.home.presentation.api.HomeUiState
import app.sensee.feature.home.presentation.impl.text.DefaultHomeTextProvider
import app.sensee.ui.designSystem.theme.SenseeTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class HomeScreenTest {
    @Test
    fun `the review widget shows the due count and daily goal`() =
        runComposeUiTest {
            setContent {
                SenseeTheme {
                    HomeContent(
                        uiState =
                            HomeUiState(
                                loadingState = DataLoadingState.Success,
                                dueCount = 5,
                                dailyGoal = 20,
                            ),
                        onAction = {},
                        textProvider = DefaultHomeTextProvider,
                    )
                }
            }

            onNodeWithText("Стоит повторить").assertIsDisplayed()
            onNodeWithText("5 карточек к повторению").assertIsDisplayed()
            onNodeWithText("Цель: 20 карточек в день").assertIsDisplayed()
        }

    @Test
    fun `the review widget shows a capped count when more is due than one session`() =
        runComposeUiTest {
            setContent {
                SenseeTheme {
                    HomeContent(
                        uiState =
                            HomeUiState(
                                loadingState = DataLoadingState.Success,
                                dueCount = 60,
                                dueExceedsSessionLimit = true,
                                dailyGoal = 20,
                            ),
                        onAction = {},
                        textProvider = DefaultHomeTextProvider,
                    )
                }
            }

            onNodeWithText("60+ карточек к повторению").assertIsDisplayed()
        }

    @Test
    fun `the CTA is disabled when nothing is due`() =
        runComposeUiTest {
            val actions = mutableListOf<HomeAction>()
            setContent {
                SenseeTheme {
                    HomeContent(
                        uiState = HomeUiState(loadingState = DataLoadingState.Success, dueCount = 0, dailyGoal = 20),
                        onAction = { actions += it },
                        textProvider = DefaultHomeTextProvider,
                    )
                }
            }

            onNodeWithText("На сегодня всё повторено").assertIsDisplayed()
            onNodeWithText("Повторить сегодня").performClick()

            assertEquals(emptyList<HomeAction>(), actions, "a disabled CTA starts no session")
        }

    @Test
    fun `tapping the CTA emits StartDueSession`() =
        runComposeUiTest {
            val actions = mutableListOf<HomeAction>()
            setContent {
                SenseeTheme {
                    HomeContent(
                        uiState = HomeUiState(loadingState = DataLoadingState.Success, dueCount = 3, dailyGoal = 20),
                        onAction = { actions += it },
                        textProvider = DefaultHomeTextProvider,
                    )
                }
            }

            onNodeWithText("Повторить сегодня").performClick()

            assertEquals(listOf<HomeAction>(HomeAction.StartDueSession), actions)
        }
}
