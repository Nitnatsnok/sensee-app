package app.sensee.ui.adaptive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.senseeLayoutMetricsFor
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
 * them like `Row` weights). Main pane bounds animate smoothly when a slot appears
 * or disappears, while trailing pane width and offset animate together so content
 * does not jump across neighbouring panes.
 *
 * On a compact layout [compactDetail] decides how the detail is presented
 * (default: it replaces the main pane with a horizontal slide).
 *
 * The detail [Child] last seen non-null is retained across the close
 * transition so the exit animation still has content to draw after
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
            sizing =
                TwoPaneSizing(
                    mainWeight = mainPaneWeight,
                    detailWeight = detailPaneWeight,
                    paneGap = LocalSenseeAdaptiveLayoutMetrics.current?.paneGap ?: 0.dp,
                ),
            animation = animation,
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
                compactDetailTransitionSpec = animation.compactDetailTransitionSpec,
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
    sizing: TwoPaneSizing,
    animation: AppChildPanelsAnimation,
    mainContent: @Composable (Child.Created<MC, MT>, Child.Created<DC, DT>?) -> Unit,
    detailContent: @Composable (Child.Created<DC, DT>, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val detailPaneContent =
        remember(retainedDetail, detailContent) {
            movableContentOf {
                if (retainedDetail != null) {
                    detailContent(retainedDetail, false)
                }
            }
        }

    WideChildPanelsLayout(
        mainWeight = sizing.mainWeight,
        paneGap = sizing.paneGap,
        supporting = false,
        animation = animation,
        mainContent = { mainContent(mainChild, detailChild) },
        trailingPanes =
            WideChildPaneList(
                listOf(
                    WideChildPane(
                        role = AppChildPaneRole.Detail,
                        visible = detailChild != null,
                        retained = retainedDetail != null,
                        weight = sizing.detailWeight,
                        content = { detailPaneContent() },
                    ),
                ),
            ),
        modifier = modifier,
    )
}

private data class TwoPaneSizing(
    val mainWeight: Float,
    val detailWeight: Float,
    val paneGap: Dp,
)

/**
 * Three-pane variant. The extra panel sits inline only on the largest layout
 * ([ContentLayoutType.SupportingPane], width ≥ Large); otherwise it goes to
 * the caller's [extra] overlay (sheet) with `compact = true`.
 *
 * Sizing uses [mainPaneWeight] / [detailPaneWeight] / [extraPaneWeight] as
 * relative weights. Inline placeholders animate via [androidx.compose.animation.animateBounds]
 * inside a [LookaheadScope] so neighbours resize smoothly, while trailing pane width
 * and offset animate together.
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
    val children =
        ThreePaneChildren(
            main = mainChild,
            detail = detailChild,
            extra = extraChild,
            retainedDetail = retainedDetail,
            retainedExtra = retainedExtra,
        )

    if (adaptiveInfo.supportsTwoPanes) {
        ThreePaneWideLayout(
            children = children,
            supporting = supporting,
            sizing =
                ThreePaneSizing(
                    mainWeight = mainPaneWeight,
                    detailWeight = detailPaneWeight,
                    extraWeight = extraPaneWeight,
                    paneGap = LocalSenseeAdaptiveLayoutMetrics.current?.paneGap ?: 0.dp,
                ),
            animation = animation,
            mainContent = main,
            detailContent = detail,
            extraContent = extra,
            modifier = modifier,
        )
    } else {
        ThreePaneCompactLayout(
            children = children,
            mainContent = main,
            detailContent = detail,
            extraContent = extra,
            compactDetail = compactDetail,
            compactDetailTransitionSpec = animation.compactDetailTransitionSpec,
            modifier = modifier,
        )
    }
}

private data class ThreePaneSizing(
    val mainWeight: Float,
    val detailWeight: Float,
    val extraWeight: Float,
    val paneGap: Dp,
)

@OptIn(ExperimentalDecomposeApi::class)
private data class ThreePaneChildren<MC : Any, MT : Any, DC : Any, DT : Any, EC : Any, ET : Any>(
    val main: Child.Created<MC, MT>,
    val detail: Child.Created<DC, DT>?,
    val extra: Child.Created<EC, ET>?,
    val retainedDetail: Child.Created<DC, DT>?,
    val retainedExtra: Child.Created<EC, ET>?,
)

@Immutable
private class WideChildPane(
    val role: AppChildPaneRole,
    val visible: Boolean,
    val retained: Boolean,
    val weight: Float,
    val content: @Composable () -> Unit,
)

@Immutable
private data class WideChildPaneList(
    val items: List<WideChildPane>,
)

private data class WideChildPanelsGeometry(
    val mainWidth: Dp,
    val trailingPanes: List<WideChildPaneGeometry>,
)

private data class WideChildPaneGeometry(
    val pane: WideChildPane,
    val width: Dp,
    val travelWidth: Dp,
    val trailingInset: Dp,
)

private data class TrailingPaneMotionTarget(
    val visible: Boolean,
    val width: Dp,
    val offset: Dp,
)

@Composable
private fun calculateWideChildPanelsGeometry(
    rowWidth: Dp,
    mainWeight: Float,
    paneGap: Dp,
    trailingPanes: WideChildPaneList,
): WideChildPanelsGeometry {
    val panes = trailingPanes.items
    val activePaneCount = panes.count { it.visible }
    val activeTrailingWeight =
        panes.fold(0f) { sum, pane ->
            sum + if (pane.visible) pane.weight else 0f
        }
    val weightSum = mainWeight + activeTrailingWeight
    val weightedWidth = (rowWidth - paneGap * activePaneCount).coerceAtLeast(0.dp)
    val activeWidths =
        panes.map { pane ->
            if (pane.visible) {
                weightedWidth * (pane.weight / weightSum)
            } else {
                0.dp
            }
        }

    return WideChildPanelsGeometry(
        mainWidth = weightedWidth * (mainWeight / weightSum),
        trailingPanes =
            panes.mapIndexed { index, pane ->
                val predictedPaneCount =
                    panes.count { it.visible || it.role == pane.role }
                val predictedWeightSum =
                    mainWeight +
                        panes.fold(0f) { sum, predictedPane ->
                            sum +
                                if (predictedPane.visible || predictedPane.role == pane.role) {
                                    predictedPane.weight
                                } else {
                                    0f
                                }
                        }
                val predictedWeightedWidth =
                    (rowWidth - paneGap * predictedPaneCount).coerceAtLeast(0.dp)
                val predictedWidth =
                    predictedWeightedWidth * (pane.weight / predictedWeightSum)
                val trailingInset =
                    panes
                        .drop(index + 1)
                        .zip(activeWidths.drop(index + 1))
                        .fold(0.dp) { inset, (trailingPane, width) ->
                            if (trailingPane.visible) {
                                inset + paneGap + width
                            } else {
                                inset
                            }
                        }

                WideChildPaneGeometry(
                    pane = pane,
                    width = activeWidths[index],
                    travelWidth =
                        rememberPaneTravelWidth(
                            visible = pane.visible,
                            retained = pane.retained,
                            activeWidth = activeWidths[index],
                            predictedWidth = predictedWidth,
                        ),
                    trailingInset = trailingInset,
                )
            },
    )
}

@OptIn(ExperimentalDecomposeApi::class)
@Composable
private fun <MC : Any, MT : Any, DC : Any, DT : Any, EC : Any, ET : Any> ThreePaneWideLayout(
    children: ThreePaneChildren<MC, MT, DC, DT, EC, ET>,
    supporting: Boolean,
    sizing: ThreePaneSizing,
    animation: AppChildPanelsAnimation,
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
        val detailPaneContent =
            remember(children.retainedDetail, children.extra, detailContent) {
                movableContentOf {
                    if (children.retainedDetail != null) {
                        detailContent(children.retainedDetail, children.extra, false)
                    }
                }
            }
        val extraPaneContent =
            remember(children.retainedExtra, extraContent) {
                movableContentOf {
                    if (children.retainedExtra != null) {
                        extraContent(children.retainedExtra, false)
                    }
                }
            }

        WideChildPanelsLayout(
            mainWeight = sizing.mainWeight,
            paneGap = sizing.paneGap,
            supporting = supporting,
            animation = animation,
            mainContent = { mainContent(children.main, children.detail, children.extra) },
            trailingPanes =
                WideChildPaneList(
                    listOf(
                        WideChildPane(
                            role = AppChildPaneRole.Detail,
                            visible = children.detail != null,
                            retained = children.retainedDetail != null,
                            weight = sizing.detailWeight,
                            content = { detailPaneContent() },
                        ),
                        WideChildPane(
                            role = AppChildPaneRole.Extra,
                            visible = supporting && children.extra != null,
                            retained = supporting && children.retainedExtra != null,
                            weight = sizing.extraWeight,
                            content = {
                                if (supporting) {
                                    extraPaneContent()
                                }
                            },
                        ),
                    ),
                ),
            modifier = Modifier.fillMaxSize(),
        )
        if (!supporting) {
            ExtraOverlay(child = children.extra, content = extraContent)
        }
    }
}

@Composable
private fun WideChildPanelsLayout(
    mainWeight: Float,
    paneGap: Dp,
    supporting: Boolean,
    animation: AppChildPanelsAnimation,
    mainContent: @Composable () -> Unit,
    trailingPanes: WideChildPaneList,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val geometry =
            calculateWideChildPanelsGeometry(
                rowWidth = maxWidth,
                mainWeight = mainWeight,
                paneGap = paneGap,
                trailingPanes = trailingPanes,
            )

        LookaheadScope {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    PaneBox(
                        role = AppChildPaneRole.Main,
                        supporting = supporting,
                        weight = mainWeight,
                        paneWidth = geometry.mainWidth,
                        lookaheadScope = this@LookaheadScope,
                        animation = animation,
                    ) {
                        mainContent()
                    }
                    geometry.trailingPanes.forEach { paneGeometry ->
                        if (paneGeometry.pane.visible && paneGeometry.pane.retained) {
                            PaneGap(
                                role = paneGeometry.pane.role,
                                supporting = supporting,
                                paneGap = paneGap,
                                lookaheadScope = this@LookaheadScope,
                                animation = animation,
                            )
                            PaneBox(
                                role = paneGeometry.pane.role,
                                supporting = supporting,
                                weight = paneGeometry.pane.weight,
                                paneWidth = paneGeometry.width,
                                lookaheadScope = this@LookaheadScope,
                                animation = animation,
                            ) {}
                        }
                    }
                }
                geometry.trailingPanes.forEach { paneGeometry ->
                    TrailingPaneOverlay(
                        role = paneGeometry.pane.role,
                        supporting = supporting,
                        visible = paneGeometry.pane.visible,
                        paneWidth = paneGeometry.travelWidth,
                        activeTrailingInset = paneGeometry.trailingInset,
                        animation = animation,
                    ) {
                        paneGeometry.pane.content()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalDecomposeApi::class)
@Composable
private fun <MC : Any, MT : Any, DC : Any, DT : Any, EC : Any, ET : Any> ThreePaneCompactLayout(
    children: ThreePaneChildren<MC, MT, DC, DT, EC, ET>,
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
    compactDetailTransitionSpec: AppChildCompactDetailTransitionSpec,
    modifier: Modifier = Modifier,
) {
    val scope =
        AppChildPanelsCompactScope(
            main = children.main,
            detail = children.detail,
            retainedDetail = children.retainedDetail,
            mainContent = { m, d -> mainContent(m, d, children.extra) },
            detailContent = { d, compact -> detailContent(d, children.extra, compact) },
            compactDetailTransitionSpec = compactDetailTransitionSpec,
        )
    Box(modifier = modifier.fillMaxSize()) {
        scope.compactDetail()
        ExtraOverlay(child = children.extra, content = extraContent)
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
    role: AppChildPaneRole,
    supporting: Boolean,
    weight: Float,
    paneWidth: Dp,
    lookaheadScope: LookaheadScope,
    animation: AppChildPanelsAnimation,
    content: @Composable () -> Unit,
) {
    val context =
        AppChildPaneMotionContext(
            role = role,
            phase = AppChildPaneMotionPhase.Resize,
            supporting = supporting,
        )
    Box(
        modifier =
            Modifier
                .animateBounds(
                    lookaheadScope = lookaheadScope,
                    modifier = Modifier.weight(weight),
                    boundsTransform = animation.boundsTransformFor(context),
                ).fillMaxHeight(),
    ) {
        PaneContent(paneWidth = paneWidth, content = content)
    }
}

@Composable
private fun BoxScope.TrailingPaneOverlay(
    role: AppChildPaneRole,
    supporting: Boolean,
    visible: Boolean,
    paneWidth: Dp,
    activeTrailingInset: Dp,
    animation: AppChildPanelsAnimation,
    content: @Composable () -> Unit,
) {
    val targetOffset = if (visible) 0.dp - activeTrailingInset else paneWidth

    fun paneTravelSpec(
        initialVisible: Boolean,
        targetVisible: Boolean,
    ): FiniteAnimationSpec<Dp> =
        animation.paneTravelSpecFor(
            AppChildPaneMotionContext(
                role = role,
                phase = paneMotionPhase(initialVisible, targetVisible),
                supporting = supporting,
            ),
        )

    val transition =
        updateTransition(
            targetState =
                TrailingPaneMotionTarget(
                    visible = visible,
                    width = paneWidth,
                    offset = targetOffset,
                ),
            label = "AppChildPanelsTrailingPane",
        )
    val animatedWidth by transition.animateDp(
        transitionSpec = { paneTravelSpec(initialState.visible, targetState.visible) },
        label = "width",
    ) { target -> target.width }
    val animatedOffset by transition.animateDp(
        transitionSpec = { paneTravelSpec(initialState.visible, targetState.visible) },
        label = "offset",
    ) { target -> target.offset }
    Box(
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = animatedOffset)
                .width(animatedWidth)
                .fillMaxHeight(),
    ) {
        PaneContent(paneWidth = animatedWidth, content = content)
    }
}

private fun paneMotionPhase(
    initialVisible: Boolean,
    targetVisible: Boolean,
): AppChildPaneMotionPhase =
    when {
        !initialVisible && targetVisible -> AppChildPaneMotionPhase.Enter
        initialVisible && !targetVisible -> AppChildPaneMotionPhase.Exit
        else -> AppChildPaneMotionPhase.Resize
    }

@Composable
private fun PaneGap(
    role: AppChildPaneRole,
    supporting: Boolean,
    paneGap: Dp,
    lookaheadScope: LookaheadScope,
    animation: AppChildPanelsAnimation,
) {
    if (paneGap <= 0.dp) return
    val context =
        AppChildPaneMotionContext(
            role = role,
            phase = AppChildPaneMotionPhase.Resize,
            supporting = supporting,
        )
    Box(
        modifier =
            Modifier
                .animateBounds(
                    lookaheadScope = lookaheadScope,
                    modifier = Modifier.width(paneGap),
                    boundsTransform = animation.boundsTransformFor(context),
                ).fillMaxHeight(),
    )
}

@Composable
private fun PaneContent(
    paneWidth: Dp,
    content: @Composable () -> Unit,
) {
    val resolved =
        LocalSenseeAdaptiveLayoutMetrics.current?.let { senseeLayoutMetricsFor(paneWidth) }
    CompositionLocalProvider(LocalSenseeAdaptiveLayoutMetrics provides resolved) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun rememberPaneTravelWidth(
    visible: Boolean,
    retained: Boolean,
    activeWidth: Dp,
    predictedWidth: Dp,
): Dp {
    var cached by remember { mutableStateOf(predictedWidth) }
    if (visible) {
        cached = activeWidth
    } else if (!retained) {
        cached = predictedWidth
    }
    return cached
}

@Composable
private fun <T : Any> DropRetainedAfterAnimation(
    active: T?,
    after: Duration,
    drop: () -> Unit,
) {
    val currentDrop by rememberUpdatedState(drop)
    // animateBounds drives the visible exit motion; once the bounds tween has
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
    private val compactDetailTransitionSpec: AppChildCompactDetailTransitionSpec,
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
            transitionSpec = { compactDetailTransitionSpec.contentTransform(this) },
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

/** Logical pane role used by [AppChildPanelsAnimation] selectors. */
public enum class AppChildPaneRole {
    Main,
    Detail,
    Extra,
}

/** Motion phase used by [AppChildPanelsAnimation] selectors. */
public enum class AppChildPaneMotionPhase {
    Enter,
    Exit,
    Resize,
}

/**
 * Describes the pane motion currently being resolved.
 *
 * [supporting] is true when the wide renderer is using the three-pane
 * supporting layout, where the extra pane is inline instead of delegated to
 * the caller-controlled overlay.
 */
@Immutable
public data class AppChildPaneMotionContext(
    val role: AppChildPaneRole,
    val phase: AppChildPaneMotionPhase,
    val supporting: Boolean,
)

/**
 * Optional role/phase override for inline pane bounds animation.
 *
 * Return `null` to use [AppChildPanelsAnimation.boundsTransform].
 */
@Stable
public fun interface AppChildPaneBoundsTransformSelector {
    public fun boundsTransform(context: AppChildPaneMotionContext): BoundsTransform?
}

/**
 * Optional role/phase override for trailing pane travel animation.
 *
 * Return `null` to use [AppChildPanelsAnimation.paneTravelSpec].
 */
@Stable
public fun interface AppChildPaneTravelSpecSelector {
    public fun paneTravelSpec(context: AppChildPaneMotionContext): FiniteAnimationSpec<Dp>?
}

/** Transition used by [AppChildPanelsCompactScope.ReplaceMainWithDetail]. */
@Stable
public fun interface AppChildCompactDetailTransitionSpec {
    public fun contentTransform(scope: AnimatedContentTransitionScope<Boolean>): ContentTransform
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

    /** Default tween used to move and resize trailing panes in sync. */
    public val PaneTravelSpec: FiniteAnimationSpec<Dp> =
        tween(durationMillis = DEFAULT_DURATION_MILLIS)

    /** No-op inline bounds selector; the base [PaneBoundsTransform] is used. */
    public val PaneBoundsTransformSelector: AppChildPaneBoundsTransformSelector =
        AppChildPaneBoundsTransformSelector { null }

    /** No-op trailing pane selector; the base [PaneTravelSpec] is used. */
    public val PaneTravelSpecSelector: AppChildPaneTravelSpecSelector =
        AppChildPaneTravelSpecSelector { null }

    /** Default compact detail transition: detail slides in, main fades out. */
    public val CompactDetailTransitionSpec: AppChildCompactDetailTransitionSpec =
        AppChildCompactDetailTransitionSpec { scope ->
            with(scope) {
                if (targetState) {
                    slideInHorizontally { width -> width } togetherWith fadeOut()
                } else {
                    fadeIn() togetherWith slideOutHorizontally { width -> width }
                }
            }
        }

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
 * Animation spec for [AppChildPanels]. [boundsTransform] drives inline pane
 * resize; [paneTravelSpec] drives trailing pane width and offset together;
 * selectors may override those defaults for a specific pane role/phase;
 * [compactDetailTransitionSpec] drives the default compact detail replacement;
 * [clearRetainedAfter] tells the renderer when the closed pane may leave the
 * layout tree (should be >= the longest visual animation duration).
 */
@Immutable
public data class AppChildPanelsAnimation(
    val boundsTransform: BoundsTransform = AppChildPanelsDefaults.PaneBoundsTransform,
    val paneTravelSpec: FiniteAnimationSpec<Dp> = AppChildPanelsDefaults.PaneTravelSpec,
    val clearRetainedAfter: Duration = AppChildPanelsDefaults.ClearRetainedAfter,
    val boundsTransformSelector: AppChildPaneBoundsTransformSelector =
        AppChildPanelsDefaults.PaneBoundsTransformSelector,
    val paneTravelSpecSelector: AppChildPaneTravelSpecSelector =
        AppChildPanelsDefaults.PaneTravelSpecSelector,
    val compactDetailTransitionSpec: AppChildCompactDetailTransitionSpec =
        AppChildPanelsDefaults.CompactDetailTransitionSpec,
) {
    internal fun boundsTransformFor(context: AppChildPaneMotionContext): BoundsTransform =
        boundsTransformSelector.boundsTransform(context) ?: boundsTransform

    internal fun paneTravelSpecFor(context: AppChildPaneMotionContext): FiniteAnimationSpec<Dp> =
        paneTravelSpecSelector.paneTravelSpec(context) ?: paneTravelSpec
}

private const val DEFAULT_MAIN_PANE_WEIGHT = 0.5f
private const val DEFAULT_DETAIL_PANE_WEIGHT = 0.5f

// Weights are not normalized to 1.0 — the geometry divides by the sum of *visible* weights,
// so the same constants give the right proportions whether two or three panes are showing.
private const val DEFAULT_MAIN_PANE_WEIGHT_3PANE = 0.3f
private const val DEFAULT_DETAIL_PANE_WEIGHT_3PANE = 0.4f
private const val DEFAULT_EXTRA_PANE_WEIGHT_3PANE = 0.6f
