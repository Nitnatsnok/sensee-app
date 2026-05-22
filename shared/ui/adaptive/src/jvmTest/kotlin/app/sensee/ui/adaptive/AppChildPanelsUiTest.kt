package app.sensee.ui.adaptive

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.arkivanov.decompose.value.MutableValue
import kotlin.test.Test

@OptIn(ExperimentalDecomposeApi::class, ExperimentalTestApi::class)
class AppChildPanelsUiTest {
    @Test
    fun `compact main pane uses latest slot content after parent recomposition`() =
        runComposeUiTest {
            val panels = MutableValue(testPanels())
            val title = mutableStateOf("Initial")

            setContent {
                CompositionLocalProvider(LocalAdaptiveInfo provides CompactAdaptiveInfo) {
                    val currentTitle = title.value

                    AppChildPanels(
                        panels = panels,
                        main = { _, _ -> BasicText(currentTitle) },
                        detail = { _, _ -> BasicText("Detail") },
                    )
                }
            }

            onNodeWithText("Initial").assertIsDisplayed()

            title.value = "Updated"
            waitForIdle()

            onNodeWithText("Updated").assertIsDisplayed()
        }
}

@OptIn(ExperimentalDecomposeApi::class)
private fun testPanels(): ChildPanels<MainConfig, MainChild, DetailConfig, DetailChild, Nothing, Nothing> =
    ChildPanels(
        main = Child.Created(MainConfig, MainChild),
        mode = ChildPanelsMode.SINGLE,
    )

private data object MainConfig

private data object DetailConfig

private data object MainChild

private data object DetailChild

private val CompactAdaptiveInfo =
    AppAdaptiveInfo(
        widthSizeClass = WidthSizeClass.Compact,
        heightSizeClass = HeightSizeClass.Medium,
    )
