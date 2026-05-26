package app.sensee.ui.adaptive

import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.arkivanov.decompose.value.MutableValue
import kotlin.math.abs
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
    fun `supporting extra pane exits at its open width while main expands`() =
        runComposeUiTest {
            val panels = MutableValue(threePanePanels(showDetail = true, showExtra = true))

            setContent {
                CompositionLocalProvider(LocalAdaptiveInfo provides SupportingAdaptiveInfo) {
                    Box(modifier = Modifier.size(width = 1000.dp, height = 500.dp)) {
                        AppChildPanels(
                            panels = panels,
                            main = { _, _, _ ->
                                Box(Modifier.fillMaxSize().testTag(MAIN_PANE_TAG))
                            },
                            detail = { _, _, _ ->
                                Box(Modifier.fillMaxSize().testTag(DETAIL_PANE_TAG))
                            },
                            extra = { child, compact ->
                                if (child != null && !compact) {
                                    Box(Modifier.fillMaxSize().testTag(EXTRA_PANE_TAG))
                                }
                            },
                        )
                    }
                }
            }
            waitForIdle()

            val initialMainBounds = onNodeWithTag(MAIN_PANE_TAG).getUnclippedBoundsInRoot()
            val initialDetailBounds = onNodeWithTag(DETAIL_PANE_TAG).getUnclippedBoundsInRoot()
            val initialExtraBounds = onNodeWithTag(EXTRA_PANE_TAG).getBoundsInRoot()

            mainClock.autoAdvance = false
            panels.value = threePanePanels(showDetail = true, showExtra = false)
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(AppChildPanelsDefaults.DEFAULT_DURATION_MILLIS / 2L)

            val exitingMainBounds = onNodeWithTag(MAIN_PANE_TAG).getUnclippedBoundsInRoot()
            val exitingDetailBounds = onNodeWithTag(DETAIL_PANE_TAG).getUnclippedBoundsInRoot()
            val exitingExtraBounds = onNodeWithTag(EXTRA_PANE_TAG).getBoundsInRoot()

            assertDpClose(
                initialExtraBounds.right - initialExtraBounds.left,
                exitingExtraBounds.right - exitingExtraBounds.left,
            )
            assertTrue(
                exitingExtraBounds.left > initialExtraBounds.left,
                "extra pane should move toward the trailing edge while exiting",
            )
            assertTrue(
                exitingMainBounds.right - exitingMainBounds.left >
                    initialMainBounds.right - initialMainBounds.left,
                "main pane should expand while the extra pane is still exiting",
            )
            assertTrue(
                exitingDetailBounds.left >= initialDetailBounds.left,
                "detail pane should not overshoot toward the main pane while extra exits",
            )
        }

    @Test
    fun `supporting extra pane enters at its open width while main shrinks`() =
        runComposeUiTest {
            val panels = MutableValue(threePanePanels(showDetail = true, showExtra = false))

            setContent {
                CompositionLocalProvider(LocalAdaptiveInfo provides SupportingAdaptiveInfo) {
                    Box(modifier = Modifier.size(width = 1000.dp, height = 500.dp)) {
                        AppChildPanels(
                            panels = panels,
                            main = { _, _, _ ->
                                Box(Modifier.fillMaxSize().testTag(MAIN_PANE_TAG))
                            },
                            detail = { _, _, _ ->
                                Box(Modifier.fillMaxSize().testTag(DETAIL_PANE_TAG))
                            },
                            extra = { child, compact ->
                                if (child != null && !compact) {
                                    Box(Modifier.fillMaxSize().testTag(EXTRA_PANE_TAG))
                                }
                            },
                        )
                    }
                }
            }
            waitForIdle()

            val initialMainBounds = onNodeWithTag(MAIN_PANE_TAG).getUnclippedBoundsInRoot()
            val expectedExtraWidth = 1000.dp * (0.6f / (0.3f + 0.4f + 0.6f))

            mainClock.autoAdvance = false
            panels.value = threePanePanels(showDetail = true, showExtra = true)
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(AppChildPanelsDefaults.DEFAULT_DURATION_MILLIS / 2L)

            val enteringMainBounds = onNodeWithTag(MAIN_PANE_TAG).getUnclippedBoundsInRoot()
            val enteringExtraBounds = onNodeWithTag(EXTRA_PANE_TAG).getUnclippedBoundsInRoot()

            assertDpClose(
                expectedExtraWidth,
                enteringExtraBounds.right - enteringExtraBounds.left,
            )
            assertTrue(
                enteringExtraBounds.left > 1000.dp - expectedExtraWidth,
                "extra pane should still be moving in from the trailing edge",
            )
            assertTrue(
                enteringMainBounds.right - enteringMainBounds.left <
                    initialMainBounds.right - initialMainBounds.left,
                "main pane should shrink while the extra pane is still entering",
            )
        }

    @Test
    fun `pane travel selector can override extra exit animation`() =
        runComposeUiTest {
            val panels = MutableValue(threePanePanels(showDetail = true, showExtra = true))
            val animation =
                AppChildPanelsAnimation(
                    paneTravelSpecSelector =
                        AppChildPaneTravelSpecSelector { context ->
                            if (
                                context.role == AppChildPaneRole.Extra &&
                                context.phase == AppChildPaneMotionPhase.Exit
                            ) {
                                snap()
                            } else {
                                null
                            }
                        },
                )

            setContent {
                CompositionLocalProvider(LocalAdaptiveInfo provides SupportingAdaptiveInfo) {
                    Box(modifier = Modifier.size(width = 1000.dp, height = 500.dp)) {
                        AppChildPanels(
                            panels = panels,
                            main = { _, _, _ ->
                                Box(Modifier.fillMaxSize().testTag(MAIN_PANE_TAG))
                            },
                            detail = { _, _, _ ->
                                Box(Modifier.fillMaxSize().testTag(DETAIL_PANE_TAG))
                            },
                            extra = { child, compact ->
                                if (child != null && !compact) {
                                    Box(Modifier.fillMaxSize().testTag(EXTRA_PANE_TAG))
                                }
                            },
                            animation = animation,
                        )
                    }
                }
            }
            waitForIdle()

            mainClock.autoAdvance = false
            panels.value = threePanePanels(showDetail = true, showExtra = false)
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()

            val exitingExtraBounds = onNodeWithTag(EXTRA_PANE_TAG).getUnclippedBoundsInRoot()

            assertTrue(
                exitingExtraBounds.left >= 1000.dp,
                "extra pane should use the role-specific snap exit override, got ${exitingExtraBounds.left}",
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

private const val MAIN_PANE_TAG = "main-pane"

private const val DETAIL_PANE_TAG = "detail-pane"

private const val EXTRA_PANE_TAG = "extra-pane"

private fun assertDpClose(
    expected: Dp,
    actual: Dp,
) {
    assertTrue(
        abs(expected.value - actual.value) < 0.5f,
        "expected $expected, got $actual",
    )
}

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
