package app.sensee.ui.learningDeck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Renders a swipeable stack of cards.
 *
 * The deck supports pointer gestures, keyboard navigation, programmatic swipes via
 * [LearningDeckState.requestSwipe], and local undo via [LearningDeckState.undo].
 *
 * Callers should provide a stable [itemKey] so the deck can reconcile its local swipe history when
 * the backing [items] list changes.
 *
 * @param items items displayed by the deck.
 * @param itemKey stable key extractor used for reconciliation and undo.
 * @param onSwipe callback invoked after the deck has locally committed a swipe.
 * @param modifier modifier applied to the deck container.
 * @param state state holder controlling current position and programmatic actions.
 * @param config gesture, stack, and animation configuration.
 * @param emptyContent content rendered when there are no visible items left, and optionally behind
 * the final card when [LearningDeckConfig.showEmptyContentBehindLastCard] is enabled.

 * @param cardContent composable used to render each visible item. The provided scope exposes both
 * per-card metadata and the current swipe state of the top card.
 */
@Composable
public fun <T, K : Any> LearningSwipeDeck(
    items: PersistentList<T>,
    itemKey: (T) -> K,
    onSwipe: (item: T, direction: LearningSwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
    state: LearningDeckState<K> = rememberLearningDeckState(),
    config: LearningDeckConfig = LearningDeckConfig.Default,
    emptyContent: @Composable BoxScope.() -> Unit = {},
    cardContent: @Composable LearningSwipeDeckCardScope.(item: T) -> Unit,
) {
    val uiCoroutineScope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val focusRequester = remember { FocusRequester() }
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    val slots = rememberLearningSwipeDeckSlots(emptyContent = emptyContent, cardContent = cardContent)
    val animationController = remember { LearningSwipeDeckAnimationController(Animatable(0f)) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val plan =
        rememberLearningSwipeDeckPlan(
            items = items,
            itemKey = itemKey,
            state = state,
            config = config,
            containerSize = containerSize,
            layoutDirection = layoutDirection,
            animationController = animationController,
        )

    LaunchedEffect(plan.currentIndex, plan.visibleCardsCount, config.keyboardEnabled) {
        if (config.keyboardEnabled && plan.visibleCardsCount > 0) {
            focusRequester.requestFocus()
        }
    }

    val actionContext =
        LearningSwipeDeckActionContext(
            uiCoroutineScope = uiCoroutineScope,
            animationController = animationController,
            state = state,
            config = config,
            layout = plan.layout,
            items = items,
            itemKey = itemKey,
            onSwipe = currentOnSwipe,
        )

    LearningSwipeRequestEffect(state = state, currentIndex = plan.currentIndex, context = actionContext)
    LearningSwipeDeckContent(
        frame =
            LearningSwipeDeckFrame(
                items = items,
                itemKey = itemKey,
                currentIndex = plan.currentIndex,
                visibleCardsCount = plan.visibleCardsCount,
            ),
        layout = plan.layout,
        deck =
            LearningSwipeDeckRenderingState(
                state = state,
                config = config,
                focusRequester = focusRequester,
                topCardSwipeState = plan.topCardSwipeState,
            ),
        slots = slots,
        interactions = actionContext.createInteractions(),
        callbacks = LearningSwipeDeckLayoutCallbacks(onContainerSizeChange = { containerSize = it }),
        modifier = modifier,
    )
}

@Composable
private fun <T, K : Any> rememberLearningSwipeDeckPlan(
    items: PersistentList<T>,
    itemKey: (T) -> K,
    state: LearningDeckState<K>,
    config: LearningDeckConfig,
    containerSize: IntSize,
    layoutDirection: LayoutDirection,
    animationController: LearningSwipeDeckAnimationController,
): LearningSwipeDeckPlan {
    val density = LocalDensity.current
    val resolvedPosition = state.resolvePosition(items = items, itemKey = itemKey)
    LaunchedEffect(resolvedPosition.index, resolvedPosition.itemKey) {
        state.syncResolvedPosition(resolvedPosition)
    }
    val currentIndex = resolvedPosition.index.coerceIn(0, items.size)
    val visibleCardsCount =
        calculateVisibleCardsCount(
            totalItems = items.size,
            currentIndex = currentIndex,
            maxVisibleCards = config.visibleCards,
        )
    val layout =
        with(density) {
            LearningSwipeDeckLayout(
                containerSize = containerSize,
                stackOffsetPx = config.stackOffset.toPx(),
                swipeThresholdPx = calculateSwipeThresholdPx(containerSize, config.swipeThresholdFraction),
                flingVelocityThresholdPx = config.flingVelocityThreshold.toPx(),
                layoutDirection = layoutDirection,
            )
        }
    val topCardSwipeState =
        animationController.resolveTopCardSwipeState(
            calculateLearningDeckSwipeState(
                offset = state.offset,
                thresholdPx = layout.swipeThresholdPx,
                layoutDirection = layoutDirection,
                allowedDirections = config.allowedDirections,
            ),
        )
    return LearningSwipeDeckPlan(
        currentIndex = currentIndex,
        visibleCardsCount = visibleCardsCount,
        layout = layout,
        topCardSwipeState = topCardSwipeState,
    )
}

private data class LearningSwipeDeckPlan(
    val currentIndex: Int,
    val visibleCardsCount: Int,
    val layout: LearningSwipeDeckLayout,
    val topCardSwipeState: LearningDeckSwipeState,
)

@Composable
private fun <T> rememberLearningSwipeDeckSlots(
    emptyContent: @Composable BoxScope.() -> Unit,
    cardContent: @Composable LearningSwipeDeckCardScope.(item: T) -> Unit,
): LearningSwipeDeckSlots<T> {
    val movableEmptyContent =
        remember(emptyContent) {
            movableContentOf<BoxScope> { boxScope ->
                with(boxScope) {
                    emptyContent()
                }
            }
        }
    val movableCardContent =
        remember(cardContent) {
            movableContentOf<LearningSwipeDeckCardScope, T> { scope, item ->
                with(scope) {
                    cardContent(item)
                }
            }
        }

    return LearningSwipeDeckSlots(
        emptyContent = movableEmptyContent,
        cardContent = movableCardContent,
    )
}

@Composable
private fun <T, K : Any> LearningSwipeRequestEffect(
    state: LearningDeckState<K>,
    currentIndex: Int,
    context: LearningSwipeDeckActionContext<T, K>,
) {
    val requestToken = state.swipeRequestId
    val currentContext by rememberUpdatedState(context)
    val currentTopIndex by rememberUpdatedState(currentIndex)

    // Key only on the request token: currentIndex/items both change *during* the swipe
    // (recordSwipe advances the index), so the LaunchedEffect must outlive their churn.
    LaunchedEffect(requestToken) {
        val direction = state.swipeRequest ?: return@LaunchedEffect
        val itemIndex = currentTopIndex
        val item = currentContext.items.getOrNull(itemIndex)
        if (item == null) {
            state.consumeSwipeRequest()
            return@LaunchedEffect
        }

        try {
            currentContext.animationController.performSwipe(
                state = state,
                config = currentContext.config,
                layout = currentContext.layout,
                execution =
                    currentContext.createSwipeExecution(
                        item = item,
                        itemIndex = itemIndex,
                    ),
                direction = direction,
            )
        } finally {
            state.consumeSwipeRequest()
        }
    }
}

@Composable
private fun <T, K : Any> LearningSwipeDeckContent(
    frame: LearningSwipeDeckFrame<T, K>,
    layout: LearningSwipeDeckLayout,
    deck: LearningSwipeDeckRenderingState<K>,
    slots: LearningSwipeDeckSlots<T>,
    interactions: LearningSwipeDeckInteractions<T>,
    callbacks: LearningSwipeDeckLayoutCallbacks,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.onSizeChanged(callbacks.onContainerSizeChange),
    ) {
        LearningSwipeDeckCards(
            frame = frame,
            layout = layout,
            deck = deck,
            slots = slots,
            interactions = interactions,
        )
    }
}

@Composable
private fun <T, K : Any> BoxScope.LearningSwipeDeckCards(
    frame: LearningSwipeDeckFrame<T, K>,
    layout: LearningSwipeDeckLayout,
    deck: LearningSwipeDeckRenderingState<K>,
    slots: LearningSwipeDeckSlots<T>,
    interactions: LearningSwipeDeckInteractions<T>,
) {
    if (frame.visibleCardsCount == 0) {
        slots.emptyContent(this)
        return
    }
    if (frame.visibleCardsCount == 1 && deck.config.showEmptyContentBehindLastCard) {
        slots.emptyContent(this)
    }
    for (stackIndex in frame.visibleCardsCount - 1 downTo 0) {
        val itemIndex = frame.currentIndex + stackIndex
        val item = frame.items[itemIndex]
        key(frame.itemKey(item)) {
            LearningSwipeDeckCard(
                stackIndex = stackIndex,
                itemIndex = itemIndex,
                item = item,
                frame = frame,
                layout = layout,
                deck = deck,
                slots = slots,
                interactions = interactions,
            )
        }
    }
}

@Composable
private fun <T, K : Any> LearningSwipeDeckCard(
    stackIndex: Int,
    itemIndex: Int,
    item: T,
    frame: LearningSwipeDeckFrame<T, K>,
    layout: LearningSwipeDeckLayout,
    deck: LearningSwipeDeckRenderingState<K>,
    slots: LearningSwipeDeckSlots<T>,
    interactions: LearningSwipeDeckInteractions<T>,
) {
    val isTopCard = stackIndex == 0
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .zIndex((frame.visibleCardsCount - stackIndex).toFloat())
                .graphicsLayer {
                    // Read offset here (deferred) so a drag only re-runs this
                    // layer block, not the whole deck composition.
                    val visualTopCardProgress =
                        if (layout.swipeThresholdPx > 0f) {
                            (deck.state.offset.distance() / layout.swipeThresholdPx)
                                .coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                    val revealIndex =
                        if (isTopCard) {
                            0f
                        } else {
                            (stackIndex - visualTopCardProgress).coerceAtLeast(0f)
                        }
                    translationX = if (isTopCard) deck.state.offset.x else 0f
                    translationY =
                        if (isTopCard) deck.state.offset.y else revealIndex * layout.stackOffsetPx
                    val width = layout.containerSize.width.coerceAtLeast(1)
                    rotationZ =
                        if (isTopCard) {
                            (deck.state.offset.x / width)
                                .coerceIn(-1f, 1f) * deck.config.maxRotationDegrees
                        } else {
                            0f
                        }
                    val scale = 1f - revealIndex * deck.config.stackScaleStep
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - revealIndex * deck.config.stackAlphaStep
                }.then(
                    if (isTopCard) {
                        Modifier.learningCardInput(
                            input =
                                LearningCardInputSpec(
                                    config = deck.config,
                                    containerSize = layout.containerSize,
                                    thresholdPx = layout.swipeThresholdPx,
                                    flingVelocityThresholdPx = layout.flingVelocityThresholdPx,
                                    layoutDirection = layout.layoutDirection,
                                ),
                            state = deck.state,
                            focusRequester = deck.focusRequester,
                            callbacks =
                                LearningCardInputCallbacks(
                                    onInteractionStart = interactions.onInteractionStart,
                                    onSettleBack = interactions.onSettleBack,
                                    onSwipe = { direction ->
                                        interactions.onSwipe(item, itemIndex, direction)
                                    },
                                ),
                        )
                    } else {
                        Modifier
                    },
                ),
    ) {
        with(
            LearningSwipeDeckCardScope(
                isTopCard = isTopCard,
                stackIndex = stackIndex,
                topCardSwipeState = deck.topCardSwipeState,
            ),
        ) {
            slots.cardContent(this, item)
        }
    }
}

private data class LearningSwipeDeckSlots<T>(
    val emptyContent: @Composable (BoxScope) -> Unit,
    val cardContent: @Composable (LearningSwipeDeckCardScope, T) -> Unit,
)

private data class LearningSwipeDeckLayoutCallbacks(
    val onContainerSizeChange: (IntSize) -> Unit,
)

private data class LearningSwipeDeckActionContext<T, K : Any>(
    val uiCoroutineScope: CoroutineScope,
    val animationController: LearningSwipeDeckAnimationController,
    val state: LearningDeckState<K>,
    val config: LearningDeckConfig,
    val layout: LearningSwipeDeckLayout,
    val items: PersistentList<T>,
    val itemKey: (T) -> K,
    val onSwipe: (item: T, direction: LearningSwipeDirection) -> Unit,
)

private fun <T, K : Any> LearningSwipeDeckActionContext<T, K>.createInteractions(): LearningSwipeDeckInteractions<T> =
    LearningSwipeDeckInteractions(
        onInteractionStart = {
            uiCoroutineScope.launch {
                animationController.startDragging()
            }
        },
        onSettleBack = {
            uiCoroutineScope.launch {
                animationController.animateTopCardBack(
                    state = state,
                    config = config,
                    layout = layout,
                )
            }
        },
        onSwipe = { item, itemIndex, direction ->
            uiCoroutineScope.launch {
                animationController.performSwipe(
                    state = state,
                    config = config,
                    layout = layout,
                    execution =
                        createSwipeExecution(
                            item = item,
                            itemIndex = itemIndex,
                        ),
                    direction = direction,
                )
            }
        },
    )

private fun <T, K : Any> LearningSwipeDeckActionContext<T, K>.createSwipeExecution(
    item: T,
    itemIndex: Int,
): LearningSwipeExecution<T, K> =
    LearningSwipeExecution(
        items = items,
        item = item,
        itemIndex = itemIndex,
        itemKey = itemKey,
        onSwipe = onSwipe,
    )

private class LearningSwipeDeckAnimationController(
    private val semanticSwipeProgress: Animatable<Float, AnimationVector1D>,
) {
    private var swipePhase by mutableStateOf(LearningDeckSwipePhase.Idle)
    private var swipePhaseDirection by mutableStateOf<LearningSwipeDirection?>(null)
    private var swipeAnimationSession = 0

    fun resolveTopCardSwipeState(rawSwipeState: LearningDeckSwipeState): LearningDeckSwipeState =
        createTopCardSwipeState(
            phase = swipePhase,
            rawSwipeState = rawSwipeState,
            pinnedDirection = swipePhaseDirection,
            pinnedProgress = semanticSwipeProgress.value,
        )

    suspend fun startDragging() {
        nextSwipeAnimationSession()
        swipePhase = LearningDeckSwipePhase.Dragging
        swipePhaseDirection = null
        semanticSwipeProgress.stop()
    }

    suspend fun animateTopCardBack(
        state: LearningDeckState<*>,
        config: LearningDeckConfig,
        layout: LearningSwipeDeckLayout,
    ) {
        if (state.isAnimating) return

        val releaseSwipe =
            calculateReleaseSwipeState(
                state = state,
                config = config,
                layout = layout,
            )
        val session = startSettlingBack(releaseSwipe)

        state.isAnimating = true
        try {
            animateBack(
                state = state,
                config = config,
            )
        } finally {
            withContext(NonCancellable) {
                resetSwipePresentation(expectedSession = session)
            }
            state.isAnimating = false
        }
    }

    suspend fun <T, K : Any> performSwipe(
        state: LearningDeckState<K>,
        config: LearningDeckConfig,
        layout: LearningSwipeDeckLayout,
        execution: LearningSwipeExecution<T, K>,
        direction: LearningSwipeDirection,
    ) {
        if (state.isAnimating) return
        if (direction !in config.allowedDirections) return
        if (layout.containerSize == IntSize.Zero) return

        val releaseSwipe =
            calculateReleaseSwipeState(
                state = state,
                config = config,
                layout = layout,
            )
        val session =
            startDismissing(
                releaseSwipe = releaseSwipe,
                direction = direction,
            )

        state.isAnimating = true
        try {
            val targetOffset =
                calculateSwipeExitTarget(
                    offset = state.offset,
                    direction = direction,
                    containerSize = layout.containerSize,
                    layoutDirection = layout.layoutDirection,
                    exitDistanceMultiplier = config.exitDistanceMultiplier,
                )
            animateDismiss(
                state = state,
                config = config,
                targetOffset = targetOffset,
            )
            recordSwipe(
                state = state,
                execution = execution,
                direction = direction,
            )
            resetSwipePresentation(expectedSession = session)
            execution.onSwipe(execution.item, direction)
        } finally {
            if (swipeAnimationSession == session) {
                withContext(NonCancellable) {
                    resetSwipePresentation(expectedSession = session)
                }
            }
            state.isAnimating = false
        }
    }

    private fun nextSwipeAnimationSession(): Int {
        swipeAnimationSession += 1
        return swipeAnimationSession
    }

    private suspend fun resetSwipePresentation(expectedSession: Int? = null) {
        if (expectedSession != null && swipeAnimationSession != expectedSession) {
            return
        }

        semanticSwipeProgress.stop()
        semanticSwipeProgress.snapTo(0f)
        swipePhase = LearningDeckSwipePhase.Idle
        swipePhaseDirection = null
    }

    private fun calculateReleaseSwipeState(
        state: LearningDeckState<*>,
        config: LearningDeckConfig,
        layout: LearningSwipeDeckLayout,
    ): LearningDeckSwipeState =
        calculateLearningDeckSwipeState(
            offset = state.offset,
            thresholdPx = layout.swipeThresholdPx,
            layoutDirection = layout.layoutDirection,
            allowedDirections = config.allowedDirections,
        )

    private suspend fun startSettlingBack(releaseSwipe: LearningDeckSwipeState): Int {
        val session = nextSwipeAnimationSession()

        semanticSwipeProgress.stop()
        semanticSwipeProgress.snapTo(releaseSwipe.progress)
        swipePhase = LearningDeckSwipePhase.SettlingBack
        swipePhaseDirection = releaseSwipe.direction
        return session
    }

    private suspend fun startDismissing(
        releaseSwipe: LearningDeckSwipeState,
        direction: LearningSwipeDirection,
    ): Int {
        val session = nextSwipeAnimationSession()
        val startingProgress = releaseSwipe.progressForDirection(direction)

        semanticSwipeProgress.stop()
        semanticSwipeProgress.snapTo(startingProgress)
        swipePhase = LearningDeckSwipePhase.Dismissing
        swipePhaseDirection = direction
        return session
    }

    private suspend fun animateBack(
        state: LearningDeckState<*>,
        config: LearningDeckConfig,
    ) {
        coroutineScope {
            launch {
                semanticSwipeProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = config.swipeStateSnapBackAnimationSpec,
                )
            }
            launch {
                state.animateBack(config.snapBackAnimationSpec)
            }
        }
    }

    private suspend fun animateDismiss(
        state: LearningDeckState<*>,
        config: LearningDeckConfig,
        targetOffset: Offset,
    ) {
        coroutineScope {
            launch {
                semanticSwipeProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = config.swipeStateDismissAnimationSpec,
                )
            }
            launch {
                state.animateOut(
                    target = targetOffset,
                    animationSpec = config.swipeOutAnimationSpec,
                )
            }
        }
    }

    private fun <T, K : Any> recordSwipe(
        state: LearningDeckState<K>,
        execution: LearningSwipeExecution<T, K>,
        direction: LearningSwipeDirection,
    ) {
        val swipedItemKey = execution.itemKey(execution.item)
        val nextItemKey = execution.items.getOrNull(execution.itemIndex + 1)?.let(execution.itemKey)

        state.recordSwipe(
            itemKey = swipedItemKey,
            itemIndex = execution.itemIndex,
            direction = direction,
            nextItemKey = nextItemKey,
        )
    }
}

private data class LearningSwipeExecution<T, K : Any>(
    val items: PersistentList<T>,
    val item: T,
    val itemIndex: Int,
    val itemKey: (T) -> K,
    val onSwipe: (item: T, direction: LearningSwipeDirection) -> Unit,
)

private data class LearningSwipeDeckFrame<T, K : Any>(
    val items: PersistentList<T>,
    val itemKey: (T) -> K,
    val currentIndex: Int,
    val visibleCardsCount: Int,
)

private data class LearningSwipeDeckLayout(
    val containerSize: IntSize,
    val stackOffsetPx: Float,
    val swipeThresholdPx: Float,
    val flingVelocityThresholdPx: Float,
    val layoutDirection: LayoutDirection,
)

private data class LearningSwipeDeckRenderingState<K : Any>(
    val state: LearningDeckState<K>,
    val config: LearningDeckConfig,
    val focusRequester: FocusRequester,
    val topCardSwipeState: LearningDeckSwipeState,
)

private data class LearningSwipeDeckInteractions<T>(
    val onInteractionStart: () -> Unit,
    val onSettleBack: () -> Unit,
    val onSwipe: (item: T, itemIndex: Int, direction: LearningSwipeDirection) -> Unit,
)

private fun createTopCardSwipeState(
    phase: LearningDeckSwipePhase,
    rawSwipeState: LearningDeckSwipeState,
    pinnedDirection: LearningSwipeDirection?,
    pinnedProgress: Float,
): LearningDeckSwipeState =
    when (phase) {
        LearningDeckSwipePhase.Idle -> LearningDeckSwipeState.Idle
        LearningDeckSwipePhase.Dragging -> rawSwipeState
        LearningDeckSwipePhase.SettlingBack,
        LearningDeckSwipePhase.Dismissing,
        -> {
            createPinnedLearningDeckSwipeState(
                phase = phase,
                direction = pinnedDirection,
                progress = pinnedProgress,
            )
        }
    }

private fun calculateVisibleCardsCount(
    totalItems: Int,
    currentIndex: Int,
    maxVisibleCards: Int,
): Int =
    (totalItems - currentIndex)
        .coerceAtLeast(0)
        .coerceAtMost(maxVisibleCards.coerceAtLeast(1))
