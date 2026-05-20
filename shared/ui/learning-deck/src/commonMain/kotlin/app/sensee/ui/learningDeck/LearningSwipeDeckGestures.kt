package app.sensee.ui.learningDeck

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import kotlinx.collections.immutable.PersistentSet
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

internal fun Modifier.learningCardInput(
    input: LearningCardInputSpec,
    state: LearningDeckState<*>,
    focusRequester: FocusRequester,
    callbacks: LearningCardInputCallbacks,
): Modifier =
    this
        .then(
            Modifier.keyboardLearningCardInput(
                enabled = input.config.keyboardEnabled,
                allowedDirections = input.config.allowedDirections,
                layoutDirection = input.layoutDirection,
                focusRequester = focusRequester,
                onSwipe = callbacks.onSwipe,
            ),
        ).then(
            Modifier.gestureLearningCardInput(
                input = input,
                state = state,
                callbacks = callbacks,
            ),
        )

private fun Modifier.keyboardLearningCardInput(
    enabled: Boolean,
    allowedDirections: PersistentSet<LearningSwipeDirection>,
    layoutDirection: LayoutDirection,
    focusRequester: FocusRequester,
    onSwipe: (LearningSwipeDirection) -> Unit,
): Modifier {
    if (!enabled) return Modifier

    return onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyUp) {
            return@onPreviewKeyEvent false
        }

        val direction =
            resolveSwipeDirectionFromKey(
                key = event.key,
                layoutDirection = layoutDirection,
            )
        if (direction != null && direction in allowedDirections) {
            onSwipe(direction)
            true
        } else {
            false
        }
    }.focusRequester(focusRequester)
        .focusable()
}

private fun Modifier.gestureLearningCardInput(
    input: LearningCardInputSpec,
    state: LearningDeckState<*>,
    callbacks: LearningCardInputCallbacks,
): Modifier {
    if (!input.config.gesturesEnabled) return Modifier

    return pointerInput(
        input.containerSize,
        input.thresholdPx,
        input.flingVelocityThresholdPx,
        input.layoutDirection,
        input.config.allowedDirections,
    ) {
        if (input.containerSize == IntSize.Zero) {
            return@pointerInput
        }

        coroutineScope {
            val velocityTracker = VelocityTracker()

            detectDragGestures(
                onDragStart = {
                    callbacks.onInteractionStart()
                    launch {
                        state.cancelOffsetAnimation()
                    }
                    velocityTracker.resetTracking()
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    velocityTracker.addPosition(
                        timeMillis = change.uptimeMillis,
                        position = change.position,
                    )
                    state.offset += dragAmount
                },
                onDragCancel = callbacks.onSettleBack,
                onDragEnd = {
                    settleSwipeFromDragEnd(
                        state = state,
                        velocity = velocityTracker.calculateVelocity(),
                        release = input.toSwipeReleaseSpec(),
                        callbacks = callbacks,
                    )
                },
            )
        }
    }
}

private fun settleSwipeFromDragEnd(
    state: LearningDeckState<*>,
    velocity: Velocity,
    release: SwipeReleaseSpec,
    callbacks: LearningCardInputCallbacks,
) {
    val direction =
        resolveSwipeDirectionFromRelease(
            offset = state.offset,
            velocity = Offset(velocity.x, velocity.y),
            release = release,
        )

    if (direction == null) {
        callbacks.onSettleBack()
    } else {
        callbacks.onSwipe(direction)
    }
}

private fun resolveSwipeDirectionFromKey(
    key: Key,
    layoutDirection: LayoutDirection,
): LearningSwipeDirection? =
    when (key) {
        Key.DirectionLeft -> {
            if (layoutDirection == LayoutDirection.Ltr) {
                LearningSwipeDirection.Start
            } else {
                LearningSwipeDirection.End
            }
        }

        Key.DirectionRight -> {
            if (layoutDirection == LayoutDirection.Ltr) {
                LearningSwipeDirection.End
            } else {
                LearningSwipeDirection.Start
            }
        }

        Key.DirectionUp -> LearningSwipeDirection.Up
        Key.DirectionDown -> LearningSwipeDirection.Down
        else -> null
    }

internal fun resolveSwipeDirectionFromRelease(
    offset: Offset,
    velocity: Offset,
    release: SwipeReleaseSpec,
): LearningSwipeDirection? {
    val directionFromVelocity =
        resolveSwipeDirectionFromVelocity(
            velocity = velocity,
            flingVelocityThresholdPx = release.flingVelocityThresholdPx,
            layoutDirection = release.layoutDirection,
            allowedDirections = release.allowedDirections,
        )

    if (directionFromVelocity != null) {
        return directionFromVelocity
    }

    return resolveSwipeDirectionFromOffset(
        offset = offset,
        thresholdPx = release.thresholdPx,
        layoutDirection = release.layoutDirection,
        allowedDirections = release.allowedDirections,
    )
}

internal data class LearningCardInputSpec(
    val config: LearningDeckConfig,
    val containerSize: IntSize,
    val thresholdPx: Float,
    val flingVelocityThresholdPx: Float,
    val layoutDirection: LayoutDirection,
)

internal data class LearningCardInputCallbacks(
    val onInteractionStart: () -> Unit,
    val onSettleBack: () -> Unit,
    val onSwipe: (LearningSwipeDirection) -> Unit,
)

internal data class SwipeReleaseSpec(
    val thresholdPx: Float,
    val flingVelocityThresholdPx: Float,
    val layoutDirection: LayoutDirection,
    val allowedDirections: PersistentSet<LearningSwipeDirection>,
)

private fun LearningCardInputSpec.toSwipeReleaseSpec(): SwipeReleaseSpec =
    SwipeReleaseSpec(
        thresholdPx = thresholdPx,
        flingVelocityThresholdPx = flingVelocityThresholdPx,
        layoutDirection = layoutDirection,
        allowedDirections = config.allowedDirections,
    )

/**
 * Tie-break convention shared with [resolveSwipeDirectionFromVelocity] and
 * `LearningDeckSwipeState`: a perfectly diagonal gesture (|x| == |y|) resolves to the
 * horizontal axis. Keep all three call sites consistent if this changes.
 */
internal fun resolveSwipeDirectionFromOffset(
    offset: Offset,
    thresholdPx: Float,
    layoutDirection: LayoutDirection,
    allowedDirections: PersistentSet<LearningSwipeDirection>,
): LearningSwipeDirection? {
    val absX = abs(offset.x)
    val absY = abs(offset.y)

    return when {
        absX >= thresholdPx && absX >= absY -> {
            resolveLogicalHorizontalDirection(
                physicalX = offset.x,
                layoutDirection = layoutDirection,
            ).takeIf { it in allowedDirections }
        }

        absY >= thresholdPx && absY > absX -> {
            val direction =
                if (offset.y < 0f) {
                    LearningSwipeDirection.Up
                } else {
                    LearningSwipeDirection.Down
                }

            direction.takeIf { it in allowedDirections }
        }

        else -> null
    }
}

internal fun calculateSwipeExitTarget(
    offset: Offset,
    direction: LearningSwipeDirection,
    containerSize: IntSize,
    layoutDirection: LayoutDirection,
    exitDistanceMultiplier: Float,
): Offset {
    val width = containerSize.width.toFloat().coerceAtLeast(1f)
    val height = containerSize.height.toFloat().coerceAtLeast(1f)
    val multiplier = exitDistanceMultiplier.coerceAtLeast(1f)

    return when (direction) {
        LearningSwipeDirection.Start,
        LearningSwipeDirection.End,
        -> {
            val sign =
                resolvePhysicalHorizontalSign(
                    direction = direction,
                    layoutDirection = layoutDirection,
                )

            Offset(
                x = sign * width * multiplier,
                y = offset.y,
            )
        }

        LearningSwipeDirection.Up -> {
            Offset(
                x = offset.x,
                y = -height * multiplier,
            )
        }

        LearningSwipeDirection.Down -> {
            Offset(
                x = offset.x,
                y = height * multiplier,
            )
        }
    }
}

internal fun calculateSwipeThresholdPx(
    containerSize: IntSize,
    thresholdFraction: Float,
): Float {
    val minDimension =
        min(
            containerSize.width,
            containerSize.height,
        )

    return minDimension *
        thresholdFraction.coerceIn(
            minimumValue = 0.05f,
            maximumValue = 0.9f,
        )
}

internal fun Offset.distance(): Float = sqrt(x * x + y * y)

private fun resolveSwipeDirectionFromVelocity(
    velocity: Offset,
    flingVelocityThresholdPx: Float,
    layoutDirection: LayoutDirection,
    allowedDirections: PersistentSet<LearningSwipeDirection>,
): LearningSwipeDirection? {
    val absX = abs(velocity.x)
    val absY = abs(velocity.y)

    return when {
        absX >= flingVelocityThresholdPx && absX >= absY -> {
            resolveLogicalHorizontalDirection(
                physicalX = velocity.x,
                layoutDirection = layoutDirection,
            ).takeIf { it in allowedDirections }
        }

        absY >= flingVelocityThresholdPx && absY > absX -> {
            val direction =
                if (velocity.y < 0f) {
                    LearningSwipeDirection.Up
                } else {
                    LearningSwipeDirection.Down
                }

            direction.takeIf { it in allowedDirections }
        }

        else -> null
    }
}

private fun resolveLogicalHorizontalDirection(
    physicalX: Float,
    layoutDirection: LayoutDirection,
): LearningSwipeDirection =
    when (layoutDirection) {
        LayoutDirection.Ltr -> {
            if (physicalX > 0f) LearningSwipeDirection.End else LearningSwipeDirection.Start
        }

        LayoutDirection.Rtl -> {
            if (physicalX > 0f) LearningSwipeDirection.Start else LearningSwipeDirection.End
        }
    }

private fun resolvePhysicalHorizontalSign(
    direction: LearningSwipeDirection,
    layoutDirection: LayoutDirection,
): Float =
    when (direction) {
        LearningSwipeDirection.End -> {
            if (layoutDirection == LayoutDirection.Ltr) 1f else -1f
        }

        LearningSwipeDirection.Start -> {
            if (layoutDirection == LayoutDirection.Ltr) -1f else 1f
        }

        LearningSwipeDirection.Up,
        LearningSwipeDirection.Down,
        -> 0f
    }
