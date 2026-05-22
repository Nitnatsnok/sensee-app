package app.sensee.ui.adaptive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.value.Value

/**
 * Adaptive renderer for a Decompose [ChildPanels] — the panels analogue of
 * Decompose's `Children`. On a wide layout ([AppAdaptiveInfo.supportsTwoPanes])
 * the main and detail panes sit side by side; on a compact layout [compactDetail]
 * decides how the detail is presented (default: it replaces the main pane with a
 * horizontal slide).
 *
 * The detail [Child] last seen non-null is retained across the close transition
 * so exit animations still have content to draw after decompose clears the slot.
 *
 * @param detail receives `compact = false` on the wide layout; the default
 * [AppChildPanelsCompactScope.ReplaceMainWithDetail] passes `compact = true`.
 */
@OptIn(ExperimentalDecomposeApi::class)
@Composable
public fun <MC : Any, MT : Any, DC : Any, DT : Any> AppChildPanels(
    panels: Value<ChildPanels<MC, MT, DC, DT, *, *>>,
    main: @Composable (main: Child.Created<MC, MT>, detail: Child.Created<DC, DT>?) -> Unit,
    detail: @Composable (detail: Child.Created<DC, DT>, compact: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    detailPaneWidthFraction: Float = DEFAULT_DETAIL_PANE_WIDTH_FRACTION,
    compactDetail: @Composable AppChildPanelsCompactScope<MC, MT, DC, DT>.() -> Unit = {
        ReplaceMainWithDetail()
    },
) {
    val childPanels by panels.subscribeAsState()
    val twoPane = LocalAdaptiveInfo.current.supportsTwoPanes

    val mainChild = childPanels.main
    val detailChild = childPanels.details

    // Retain the last opened detail child so a close/exit transition still has
    // content to render after decompose has cleared panels.details.
    var retained by remember { mutableStateOf(detailChild) }
    if (detailChild != null) {
        retained = detailChild
    }

    if (twoPane) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val detailWidth = maxWidth * detailPaneWidthFraction
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    main(mainChild, detailChild)
                }
                AnimatedVisibility(visible = detailChild != null) {
                    val shown = retained
                    if (shown != null) {
                        Box(modifier = Modifier.width(detailWidth).fillMaxHeight()) {
                            detail(shown, false)
                        }
                    }
                }
            }
        }
    } else {
        val scope =
            AppChildPanelsCompactScope(
                main = mainChild,
                detail = detailChild,
                retainedDetail = retained,
                mainContent = main,
                detailContent = detail,
            )
        Box(modifier = modifier.fillMaxSize()) {
            scope.compactDetail()
        }
    }
}

/**
 * Receiver for [AppChildPanels]'s `compactDetail` slot. Exposes the current
 * panel children and the main/detail content so a screen can compose its own
 * compact presentation (full-screen replacement, bottom sheet, …).
 */
@OptIn(ExperimentalDecomposeApi::class)
public class AppChildPanelsCompactScope<MC : Any, MT : Any, DC : Any, DT : Any> internal constructor(
    public val main: Child.Created<MC, MT>,
    public val detail: Child.Created<DC, DT>?,
    private val retainedDetail: Child.Created<DC, DT>?,
    private val mainContent: @Composable (Child.Created<MC, MT>, Child.Created<DC, DT>?) -> Unit,
    private val detailContent: @Composable (Child.Created<DC, DT>, Boolean) -> Unit,
) {
    /** The main pane content, as supplied to [AppChildPanels]. */
    @Composable
    public fun MainPane() {
        mainContent(main, detail)
    }

    /** The detail pane content for [child], rendered in compact mode. */
    @Composable
    public fun DetailPane(child: Child.Created<DC, DT>) {
        detailContent(child, true)
    }

    /**
     * Default compact behaviour — the detail pane replaces the main pane, with
     * the detail sliding in from and out toward the trailing edge.
     */
    @Composable
    public fun ReplaceMainWithDetail(modifier: Modifier = Modifier) {
        AnimatedContent(
            targetState = detail != null,
            modifier = modifier.fillMaxSize(),
            transitionSpec = {
                if (targetState) {
                    slideInHorizontally { width -> width } togetherWith fadeOut()
                } else {
                    fadeIn() togetherWith slideOutHorizontally { width -> width }
                }
            },
            label = "AppChildPanelsCompact",
        ) { hasDetail ->
            val shown = retainedDetail
            if (hasDetail && shown != null) {
                DetailPane(shown)
            } else {
                MainPane()
            }
        }
    }
}

private const val DEFAULT_DETAIL_PANE_WIDTH_FRACTION = 0.6f
