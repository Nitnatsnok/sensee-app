package app.sensee.ui.adaptive

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.arkivanov.decompose.value.MutableValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun `three-pane supporting layout renders all three panes inline`() =
        runComposeUiTest {
            val panels = MutableValue(threePanePanels(showDetail = true, showExtra = true))
            val extraCompactFlags = mutableListOf<Boolean>()

            setContent {
                CompositionLocalProvider(LocalAdaptiveInfo provides SupportingAdaptiveInfo) {
                    AppChildPanels(
                        panels = panels,
                        main = { _, _, _ -> BasicText("Main") },
                        detail = { _, _, _ -> BasicText("Detail") },
                        extra = { child, compact ->
                            extraCompactFlags += compact
                            if (child != null) BasicText("Extra")
                        },
                    )
                }
            }

            onNodeWithText("Main").assertIsDisplayed()
            onNodeWithText("Detail").assertIsDisplayed()
            onNodeWithText("Extra").assertIsDisplayed()
            assertTrue(
                extraCompactFlags.any { !it },
                "supporting-pane layout passes compact = false to the extra slot",
            )
        }

    @Test
    fun `list-detail layout keeps main and detail inline and delegates extra to the caller overlay`() =
        runComposeUiTest {
            val panels = MutableValue(threePanePanels(showDetail = true, showExtra = true))
            val extraCompactFlags = mutableListOf<Boolean>()
            val extraChildPresent = mutableListOf<Boolean>()

            setContent {
                CompositionLocalProvider(LocalAdaptiveInfo provides ListDetailAdaptiveInfo) {
                    AppChildPanels(
                        panels = panels,
                        main = { _, _, _ -> BasicText("Main") },
                        detail = { _, _, _ -> BasicText("Detail") },
                        extra = { child, compact ->
                            extraCompactFlags += compact
                            extraChildPresent += (child != null)
                            // Caller-controlled overlay: the test sees the lambda
                            // is invoked but renders no inline node.
                        },
                    )
                }
            }

            onNodeWithText("Main").assertIsDisplayed()
            onNodeWithText("Detail").assertIsDisplayed()
            onAllNodesWithText("Extra").assertCountEquals(0)
            assertEquals(
                true,
                extraCompactFlags.all { it },
                "list-detail layout passes compact = true to the extra slot",
            )
            assertTrue(
                extraChildPresent.any { it },
                "list-detail layout still forwards the active extra child to the caller",
            )
        }

    @Test
    fun `compact layout keeps extra slot driven by the caller`() =
        runComposeUiTest {
            val panels = MutableValue(threePanePanels(showDetail = false, showExtra = true))
            val extraCompactFlags = mutableListOf<Boolean>()

            setContent {
                CompositionLocalProvider(LocalAdaptiveInfo provides CompactAdaptiveInfo) {
                    AppChildPanels(
                        panels = panels,
                        main = { _, _, _ -> BasicText("Main") },
                        detail = { _, _, _ -> BasicText("Detail") },
                        extra = { _, compact ->
                            extraCompactFlags += compact
                        },
                    )
                }
            }

            onNodeWithText("Main").assertIsDisplayed()
            onAllNodesWithText("Detail").assertCountEquals(0)
            assertEquals(
                true,
                extraCompactFlags.all { it },
                "compact layout passes compact = true to the extra slot",
            )
        }
}

@OptIn(ExperimentalDecomposeApi::class)
private fun testPanels(): ChildPanels<MainConfig, MainChild, DetailConfig, DetailChild, Nothing, Nothing> =
    ChildPanels(
        main = Child.Created(MainConfig, MainChild),
        mode = ChildPanelsMode.SINGLE,
    )

@OptIn(ExperimentalDecomposeApi::class)
private fun threePanePanels(
    showDetail: Boolean,
    showExtra: Boolean,
): ChildPanels<MainConfig, MainChild, DetailConfig, DetailChild, ExtraConfig, ExtraChild> =
    ChildPanels(
        main = Child.Created(MainConfig, MainChild),
        details = if (showDetail) Child.Created(DetailConfig, DetailChild) else null,
        extra = if (showExtra) Child.Created(ExtraConfig, ExtraChild) else null,
        mode = ChildPanelsMode.SINGLE,
    )

private data object MainConfig

private data object DetailConfig

private data object ExtraConfig

private data object MainChild

private data object DetailChild

private data object ExtraChild

private val CompactAdaptiveInfo =
    AppAdaptiveInfo(
        widthSizeClass = WidthSizeClass.Compact,
        heightSizeClass = HeightSizeClass.Medium,
    )

private val ListDetailAdaptiveInfo =
    AppAdaptiveInfo(
        widthSizeClass = WidthSizeClass.Expanded,
        heightSizeClass = HeightSizeClass.Expanded,
    )

private val SupportingAdaptiveInfo =
    AppAdaptiveInfo(
        widthSizeClass = WidthSizeClass.Large,
        heightSizeClass = HeightSizeClass.Expanded,
    )
