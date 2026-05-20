package app.sensee.ui.learningDeck

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.collections.immutable.PersistentSet
import kotlin.math.max

/**
 * High-level semantic phase of the top card's swipe interaction.
 */
public enum class LearningDeckSwipePhase {
    /** The top card is resting and not presenting any swipe interaction. */
    Idle,

    /** The user is actively dragging the top card. */
    Dragging,

    /** The user released an incomplete drag and the card is settling back to rest. */
    SettlingBack,

    /** The top card is dismissing after a completed swipe. */
    Dismissing,
}

/**
 * Describes the current swipe presentation of the top card in a [LearningSwipeDeck].
 *
 * The state is exposed to every visible card through
 * [LearningSwipeDeckCardScope.topCardSwipeState], which lets card content react to the active
 * gesture without reading the deck's internal state.
 *
 * Each directional progress is normalized to the `0f..1f` range relative to the deck's swipe
 * threshold and only reports directions that are allowed by the active [LearningDeckConfig].
 *
 * During [LearningDeckSwipePhase.SettlingBack] and [LearningDeckSwipePhase.Dismissing], the
 * direction is semantically pinned to the interaction that triggered the phase. This prevents card
 * overlays from reacting to physical overshoot or direction changes in the underlying animation.
 */
@Immutable
public data class LearningDeckSwipeState(
    val phase: LearningDeckSwipePhase,
    val progress: Float,
    /**
     * Dominant or pinned swipe direction of the top card.
     *
     * It can be `null` when the card is idle, or when the current physical movement does not map
     * to an allowed direction.
     */
    val direction: LearningSwipeDirection?,
    val startProgress: Float,
    val endProgress: Float,
    val upProgress: Float,
    val downProgress: Float,
) {
    val isIdle: Boolean
        get() = phase == LearningDeckSwipePhase.Idle

    val isDragging: Boolean
        get() = phase == LearningDeckSwipePhase.Dragging

    val isSettlingBack: Boolean
        get() = phase == LearningDeckSwipePhase.SettlingBack

    val isDismissing: Boolean
        get() = phase == LearningDeckSwipePhase.Dismissing

    val isActive: Boolean
        get() = !isIdle

    /** `true` while the semantic swipe state is driven by an animation rather than direct drag. */
    val isAnimating: Boolean
        get() = isSettlingBack || isDismissing

    public companion object {
        public val Idle: LearningDeckSwipeState =
            LearningDeckSwipeState(
                phase = LearningDeckSwipePhase.Idle,
                progress = 0f,
                direction = null,
                startProgress = 0f,
                endProgress = 0f,
                upProgress = 0f,
                downProgress = 0f,
            )
    }
}

internal fun calculateLearningDeckSwipeState(
    offset: Offset,
    thresholdPx: Float,
    layoutDirection: LayoutDirection,
    allowedDirections: PersistentSet<LearningSwipeDirection>,
): LearningDeckSwipeState {
    if (thresholdPx <= 0f || allowedDirections.isEmpty()) {
        return LearningDeckSwipeState.Idle
    }

    val logicalHorizontalOffset =
        toLogicalHorizontalOffset(
            physicalX = offset.x,
            layoutDirection = layoutDirection,
        )
    val swipeProgress =
        calculateDirectionalSwipeProgress(
            logicalHorizontalOffset = logicalHorizontalOffset,
            verticalOffset = offset.y,
            thresholdPx = thresholdPx,
            allowedDirections = allowedDirections,
        )
    val direction = swipeProgress.direction ?: return LearningDeckSwipeState.Idle

    return LearningDeckSwipeState(
        phase = LearningDeckSwipePhase.Dragging,
        progress = swipeProgress.progress,
        direction = direction,
        startProgress = swipeProgress.start,
        endProgress = swipeProgress.end,
        upProgress = swipeProgress.up,
        downProgress = swipeProgress.down,
    )
}

private data class DirectionalSwipeProgress(
    val start: Float,
    val end: Float,
    val up: Float,
    val down: Float,
) {
    private val horizontal: Float = max(start, end)
    private val vertical: Float = max(up, down)

    val progress: Float = max(horizontal, vertical)
    val direction: LearningSwipeDirection? =
        when {
            progress <= 0f -> null
            horizontal >= vertical -> {
                if (end >= start) {
                    LearningSwipeDirection.End
                } else {
                    LearningSwipeDirection.Start
                }
            }

            down >= up -> LearningSwipeDirection.Down
            else -> LearningSwipeDirection.Up
        }
}

private fun calculateDirectionalSwipeProgress(
    logicalHorizontalOffset: Float,
    verticalOffset: Float,
    thresholdPx: Float,
    allowedDirections: PersistentSet<LearningSwipeDirection>,
): DirectionalSwipeProgress =
    DirectionalSwipeProgress(
        start =
            directionalProgressForDirection(
                direction = LearningSwipeDirection.Start,
                distance = -logicalHorizontalOffset,
                thresholdPx = thresholdPx,
                allowedDirections = allowedDirections,
            ),
        end =
            directionalProgressForDirection(
                direction = LearningSwipeDirection.End,
                distance = logicalHorizontalOffset,
                thresholdPx = thresholdPx,
                allowedDirections = allowedDirections,
            ),
        up =
            directionalProgressForDirection(
                direction = LearningSwipeDirection.Up,
                distance = -verticalOffset,
                thresholdPx = thresholdPx,
                allowedDirections = allowedDirections,
            ),
        down =
            directionalProgressForDirection(
                direction = LearningSwipeDirection.Down,
                distance = verticalOffset,
                thresholdPx = thresholdPx,
                allowedDirections = allowedDirections,
            ),
    )

internal fun createPinnedLearningDeckSwipeState(
    phase: LearningDeckSwipePhase,
    direction: LearningSwipeDirection?,
    progress: Float,
): LearningDeckSwipeState {
    if (phase == LearningDeckSwipePhase.Idle) {
        return LearningDeckSwipeState.Idle
    }

    val resolvedProgress = progress.coerceIn(0f, 1f)
    return LearningDeckSwipeState(
        phase = phase,
        progress = resolvedProgress,
        direction = direction,
        startProgress = if (direction == LearningSwipeDirection.Start) resolvedProgress else 0f,
        endProgress = if (direction == LearningSwipeDirection.End) resolvedProgress else 0f,
        upProgress = if (direction == LearningSwipeDirection.Up) resolvedProgress else 0f,
        downProgress = if (direction == LearningSwipeDirection.Down) resolvedProgress else 0f,
    )
}

internal fun LearningDeckSwipeState.progressForDirection(direction: LearningSwipeDirection?): Float =
    when (direction) {
        LearningSwipeDirection.Start -> startProgress
        LearningSwipeDirection.End -> endProgress
        LearningSwipeDirection.Up -> upProgress
        LearningSwipeDirection.Down -> downProgress
        null -> 0f
    }

private fun directionalProgress(
    distance: Float,
    thresholdPx: Float,
): Float {
    if (distance <= 0f) {
        return 0f
    }

    return (distance / thresholdPx).coerceIn(0f, 1f)
}

private fun directionalProgressForDirection(
    direction: LearningSwipeDirection,
    distance: Float,
    thresholdPx: Float,
    allowedDirections: PersistentSet<LearningSwipeDirection>,
): Float =
    if (direction in allowedDirections) {
        directionalProgress(
            distance = distance,
            thresholdPx = thresholdPx,
        )
    } else {
        0f
    }

private fun toLogicalHorizontalOffset(
    physicalX: Float,
    layoutDirection: LayoutDirection,
): Float =
    when (layoutDirection) {
        LayoutDirection.Ltr -> physicalX
        LayoutDirection.Rtl -> -physicalX
    }
