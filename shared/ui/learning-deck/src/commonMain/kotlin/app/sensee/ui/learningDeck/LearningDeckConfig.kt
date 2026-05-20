package app.sensee.ui.learningDeck

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf

/**
 * Configures gesture handling, stack appearance, and animations for [LearningSwipeDeck].
 *
 * The defaults are tuned for compact learning-card stacks, while still exposing the parts that
 * most affect interaction feel and visual density.
 */
@Stable
public data class LearningDeckConfig(
    val visibleCards: Int = 3,
    val swipeThresholdFraction: Float = 0.28f,
    val flingVelocityThreshold: Dp = 900.dp,
    val stackOffset: Dp = 14.dp,
    val stackScaleStep: Float = 0.045f,
    val stackAlphaStep: Float = 0.08f,
    val maxRotationDegrees: Float = 12f,
    val exitDistanceMultiplier: Float = 1.35f,
    val gesturesEnabled: Boolean = true,
    val keyboardEnabled: Boolean = true,
    val swipeOutAnimationSpec: FiniteAnimationSpec<Offset> =
        LearningCardsDefaults
            .swipeOutAnimationSpec(),
    val snapBackAnimationSpec: FiniteAnimationSpec<Offset> =
        LearningCardsDefaults
            .snapBackAnimationSpec(),
    val swipeStateSnapBackAnimationSpec: FiniteAnimationSpec<Float> =
        LearningCardsDefaults
            .swipeStateSnapBackAnimationSpec(),
    val swipeStateDismissAnimationSpec: FiniteAnimationSpec<Float> =
        LearningCardsDefaults
            .swipeStateDismissAnimationSpec(),
    /**
     * When `true`, [LearningSwipeDeck]'s [emptyContent][LearningSwipeDeck] is also composed behind
     * the final visible card so the deck transitions into its empty state less abruptly.
     */
    val showEmptyContentBehindLastCard: Boolean = true,
    val allowedDirections: PersistentSet<LearningSwipeDirection> =
        persistentSetOf(
            LearningSwipeDirection.Start,
            LearningSwipeDirection.End,
            LearningSwipeDirection.Up,
            LearningSwipeDirection.Down,
        ),
) {
    public companion object {
        /**
         * Shared default instance. Use this instead of `LearningDeckConfig()` as a default
         * argument: the constructor allocates fresh animation specs / sets on every call, so a
         * per-recomposition `LearningDeckConfig()` would undermine the `@Stable` promise.
         */
        public val Default: LearningDeckConfig = LearningDeckConfig()
    }
}
