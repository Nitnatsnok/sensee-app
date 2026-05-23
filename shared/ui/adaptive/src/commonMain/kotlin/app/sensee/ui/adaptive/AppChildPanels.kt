package app.sensee.ui.adaptive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Adaptive renderer for a Decompose [ChildPanels] — the panels analogue of
 * Decompose's `Children`. On a wide layout ([AppAdaptiveInfo.supportsTwoPanes])
 * main and detail panes sit side by side; pane sizes come from
 * [mainPaneWeight] / [detailPaneWeight] (relative weights — Compose normalizes
 * them like `Row` weights) and pane bounds animate smoothly when a slot
 * appears or disappears thanks to [androidx.compose.animation.animateBounds]
 * inside a [LookaheadScope].
 *
 * On a compact layout [compactDetail] decides how the detail is presented
 * (default: it replaces the main pane with a horizontal slide).
 *
 * The detail [Child] last seen non-null is retained across the close
 * transition so the exit-shrink animation still has content to draw after
 * decompose clears the slot.
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
    mainPaneWeight: Float = DEFAULT_MAIN_PANE_WEIGHT,
    detailPaneWeight: Float = DEFAULT_DETAIL_PANE_WEIGHT,
    animation: AppChildPanelsAnimation = AppChildPanelsAnimation(),
    compactDetail: @Composable AppChildPanelsCompactScope<MC, MT, DC, DT>.() -> Unit = {
        ReplaceMainWithDetail()
    },
) {
    val childPanels by panels.subscribeAsState()
    val twoPane = LocalAdaptiveInfo.current.supportsTwoPanes

    val mainChild = childPanels.main
    val detailChild = childPanels.details

    var retained by remember { mutableStateOf(detailChild) }
    if (detailChild != null) {
        retained = detailChild
    }
    DropRetainedAfterAnimation(active = detailChild, after = animation.clearRetainedAfter) {
        retained = null
    }

    if (twoPane) {
        TwoPaneWideRow(
            mainChild = mainChild,
            detailChild = detailChild,
            retainedDetail = retained,
            mainPaneWeight = mainPaneWeight,
            detailPaneWeight = detailPaneWeight,
            boundsTransform = animation.boundsTransform,
            mainContent = main,
            detailContent = detail,
            modifier = modifier,
        )
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

@OptIn(ExperimentalDecomposeApi::class)
@Composable
private fun <MC : Any, MT : Any, DC : Any, DT : Any> TwoPaneWideRow(
    mainChild: Child.Created<MC, MT>,
    detailChild: Child.Created<DC, DT>?,
    retainedDetail: Child.Created<DC, DT>?,
    mainPaneWeight: Float,
    detailPaneWeight: Float,
    boundsTransform: BoundsTransform,
    mainContent: @Composable (Child.Created<MC, MT>, Child.Created<DC, DT>?) -> Unit,
    detailContent: @Composable (Child.Created<DC, DT>, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LookaheadScope {
        Row(modifier = modifier.fillMaxSize()) {
            PaneBox(
                weight = mainPaneWeight,
                lookaheadScope = this@LookaheadScope,
                boundsTransform = boundsTransform,
            ) {
                mainContent(mainChild, detailChild)
            }
            if (retainedDetail != null) {
                PaneBox(
                    weight = if (detailChild != null) detailPaneWeight else SLOT_HIDDEN_WEIGHT,
                    lookaheadScope = this@LookaheadScope,
                    boundsTransform = boundsTransform,
                ) {
                    detailContent(retainedDetail, false)
                }
            }
        }
    }
}

/**
 * Three-pane variant. The extra panel sits inline only on the largest layout
 * ([ContentLayoutType.SupportingPane], width ≥ Large); otherwise it goes to
 * the caller's [extra] overlay (sheet) with `compact = true`.
 *
 * Sizing uses [mainPaneWeight] / [detailPaneWeight] / [extraPaneWeight] as
 * relative weights; bounds animate via [androidx.compose.animation.animateBounds]
 * inside a [LookaheadScope] so opening or closing a pane smoothly resizes the
 * neighbours.
 */
@OptIn(ExperimentalDecomposeApi::class)
@Composable
public fun <MC : Any, MT : Any, DC : Any, DT : Any, EC : Any, ET : Any> AppChildPanels(
    panels: Value<ChildPanels<MC, MT, DC, DT, EC, ET>>,
    main: @Composable (
        main: Child.Created<MC, MT>,
        detail: Child.Created<DC, DT>?,
        extra: Child.Created<EC, ET>?,
    ) -> Unit,
    detail: @Composable (
        detail: Child.Created<DC, DT>,
        extra: Child.Created<EC, ET>?,
        compact: Boolean,
    ) -> Unit,
    extra: @Composable (child: Child.Created<EC, ET>?, compact: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    mainPaneWeight: Float = DEFAULT_MAIN_PANE_WEIGHT_3PANE,
    detailPaneWeight: Float = DEFAULT_DETAIL_PANE_WEIGHT_3PANE,
    extraPaneWeight: Float = DEFAULT_EXTRA_PANE_WEIGHT_3PANE,
    animation: AppChildPanelsAnimation = AppChildPanelsAnimation(),
    compactDetail: @Composable AppChildPanelsCompactScope<MC, MT, DC, DT>.() -> Unit = {
        ReplaceMainWithDetail()
    },
) {
    val childPanels by panels.subscribeAsState()
    val adaptiveInfo = LocalAdaptiveInfo.current

    val mainChild = childPanels.main
    val detailChild = childPanels.details
    val extraChild = childPanels.extra

    var retainedDetail by remember { mutableStateOf(detailChild) }
    if (detailChild != null) {
        retainedDetail = detailChild
    }
    DropRetainedAfterAnimation(active = detailChild, after = animation.clearRetainedAfter) {
        retainedDetail = null
    }

    var retainedExtra by remember { mutableStateOf(extraChild) }
    if (extraChild != null) {
        retainedExtra = extraChild
    }
    DropRetainedAfterAnimation(active = extraChild, after = animation.clearRetainedAfter) {
        retainedExtra = null
    }

    val supporting = adaptiveInfo.contentLayoutType == ContentLayoutType.SupportingPane

    if (adaptiveInfo.supportsTwoPanes) {
        ThreePaneWideLayout(
            children =
                ThreePaneChildren(
                    main = mainChild,
                    detail = detailChild,
                    extra = extraChild,
                    retainedDetail = retainedDetail,
                    retainedExtra = retainedExtra,
                ),
            supporting = supporting,
            weights = ThreePaneWeights(mainPaneWeight, detailPaneWeight, extraPaneWeight),
            boundsTransform = animation.boundsTransform,
            mainContent = main,
            detailContent = detail,
            extraContent = extra,
            modifier = modifier,
        )
    } else {
        ThreePaneCompactLayout(
            mainChild = mainChild,
            detailChild = detailChild,
            extraChild = extraChild,
            retainedDetail = retainedDetail,
            mainContent = main,
            detailContent = detail,
            extraContent = extra,
            compactDetail = compactDetail,
            modifier = modifier,
        )
    }
}

private data class ThreePaneWeights(
    val main: Float,
    val detail: Float,
    val extra: Float,
)

@OptIn(ExperimentalDecomposeApi::class)
private data class ThreePaneChildren<MC : Any, MT : Any, DC : Any, DT : Any, EC : Any, ET : Any>(
    val main: Child.Created<MC, MT>,
    val detail: Child.Created<DC, DT>?,
    val extra: Child.Created<EC, ET>?,
    val retainedDetail: Child.Created<DC, DT>?,
    val retainedExtra: Child.Created<EC, ET>?,
)

@OptIn(ExperimentalDecomposeApi::class)
@Composable
private fun <MC : Any, MT : Any, DC : Any, DT : Any, EC : Any, ET : Any> ThreePaneWideLayout(
    children: ThreePaneChildren<MC, MT, DC, DT, EC, ET>,
    supporting: Boolean,
    weights: ThreePaneWeights,
    boundsTransform: BoundsTransform,
    mainContent: @Composable (
        Child.Created<MC, MT>,
        Child.Created<DC, DT>?,
        Child.Created<EC, ET>?,
    ) -> Unit,
    detailContent: @Composable (
        Child.Created<DC, DT>,
        Child.Created<EC, ET>?,
        Boolean,
    ) -> Unit,
    extraContent: @Composable (Child.Created<EC, ET>?, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LookaheadScope {
            Row(modifier = Modifier.fillMaxSize()) {
                PaneBox(
                    weight = weights.main,
                    lookaheadScope = this@LookaheadScope,
                    boundsTransform = boundsTransform,
                ) {
                    mainContent(children.main, children.detail, children.extra)
                }
                if (children.retainedDetail != null) {
                    PaneBox(
                        weight = if (children.detail != null) weights.detail else SLOT_HIDDEN_WEIGHT,
                        lookaheadScope = this@LookaheadScope,
                        boundsTransform = boundsTransform,
                    ) {
                        detailContent(children.retainedDetail, children.extra, false)
                    }
                }
                if (supporting && children.retainedExtra != null) {
                    PaneBox(
                        weight = if (children.extra != null) weights.extra else SLOT_HIDDEN_WEIGHT,
                        lookaheadScope = this@LookaheadScope,
                        boundsTransform = boundsTransform,
                    ) {
                        extraContent(children.retainedExtra, false)
                    }
                }
            }
        }
        if (!supporting) {
            ExtraOverlay(child = children.extra, content = extraContent)
        }
    }
}

@OptIn(ExperimentalDecomposeApi::class)
@Composable
private fun <MC : Any, MT : Any, DC : Any, DT : Any, EC : Any, ET : Any> ThreePaneCompactLayout(
    mainChild: Child.Created<MC, MT>,
    detailChild: Child.Created<DC, DT>?,
    extraChild: Child.Created<EC, ET>?,
    retainedDetail: Child.Created<DC, DT>?,
    mainContent: @Composable (
        Child.Created<MC, MT>,
        Child.Created<DC, DT>?,
        Child.Created<EC, ET>?,
    ) -> Unit,
    detailContent: @Composable (
        Child.Created<DC, DT>,
        Child.Created<EC, ET>?,
        Boolean,
    ) -> Unit,
    extraContent: @Composable (Child.Created<EC, ET>?, Boolean) -> Unit,
    compactDetail: @Composable AppChildPanelsCompactScope<MC, MT, DC, DT>.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope =
        AppChildPanelsCompactScope(
            main = mainChild,
            detail = detailChild,
            retainedDetail = retainedDetail,
            mainContent = { m, d -> mainContent(m, d, extraChild) },
            detailContent = { d, compact -> detailContent(d, extraChild, compact) },
        )
    Box(modifier = modifier.fillMaxSize()) {
        scope.compactDetail()
        ExtraOverlay(child = extraChild, content = extraContent)
    }
}

@OptIn(ExperimentalDecomposeApi::class)
@Composable
private fun <EC : Any, ET : Any> ExtraOverlay(
    child: Child.Created<EC, ET>?,
    content: @Composable (Child.Created<EC, ET>?, Boolean) -> Unit,
) {
    // Caller-controlled overlay (typically a SenseeModalBottomSheet driven by
    // `child != null`); the lambda is invoked even when the slot is closed so
    // the sheet has a frame to animate out in.
    content(child, true)
}

@Composable
private fun RowScope.PaneBox(
    weight: Float,
    lookaheadScope: LookaheadScope,
    boundsTransform: BoundsTransform,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .animateBounds(
                    lookaheadScope = lookaheadScope,
                    modifier = Modifier.weight(weight),
                    boundsTransform = boundsTransform,
                ).fillMaxHeight(),
    ) {
        content()
    }
}

@Composable
private fun <T : Any> DropRetainedAfterAnimation(
    active: T?,
    after: Duration,
    drop: () -> Unit,
) {
    val currentDrop by rememberUpdatedState(drop)
    // animateBounds drives the visible exit-shrink; once the bounds tween has
    // settled (with a small grace) the slot may leave the tree.
    LaunchedEffect(active) {
        if (active == null) {
            delay(after)
            currentDrop()
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

/**
 * Defaults for [AppChildPanels]. Exposed so callers can build a custom
 * [AppChildPanelsAnimation] by overriding only one field while keeping the
 * other at its default.
 */
public object AppChildPanelsDefaults {
    /** Default tween used to interpolate pane bounds. */
    public val PaneBoundsTransform: BoundsTransform =
        BoundsTransform { _, _ -> tween(durationMillis = DEFAULT_DURATION_MILLIS) }

    /**
     * Delay before a closed pane leaves the layout tree, so the bounds tween
     * has time to finish its visual exit-shrink. Default sits slightly above
     * the bounds tween duration to absorb any frame-budget slack.
     */
    public val ClearRetainedAfter: Duration =
        (DEFAULT_DURATION_MILLIS + DEFAULT_GRACE_MILLIS).milliseconds

    internal const val DEFAULT_DURATION_MILLIS = 320
    internal const val DEFAULT_GRACE_MILLIS = 60
}

/**
 * Animation spec for [AppChildPanels]. [boundsTransform] drives the visual
 * shrink/grow of each pane; [clearRetainedAfter] tells the renderer when the
 * closed pane may leave the layout tree (should be ≥ the bounds animation's
 * own duration).
 */
@Immutable
public data class AppChildPanelsAnimation(
    val boundsTransform: BoundsTransform = AppChildPanelsDefaults.PaneBoundsTransform,
    val clearRetainedAfter: Duration = AppChildPanelsDefaults.ClearRetainedAfter,
)

private const val SLOT_HIDDEN_WEIGHT = 0.0001f
private const val DEFAULT_MAIN_PANE_WEIGHT = 0.5f
private const val DEFAULT_DETAIL_PANE_WEIGHT = 0.5f
private const val DEFAULT_MAIN_PANE_WEIGHT_3PANE = 0.3f
private const val DEFAULT_DETAIL_PANE_WEIGHT_3PANE = 0.4f
private const val DEFAULT_EXTRA_PANE_WEIGHT_3PANE = 0.6f
