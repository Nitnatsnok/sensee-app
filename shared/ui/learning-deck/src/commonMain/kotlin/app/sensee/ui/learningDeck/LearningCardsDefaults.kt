package app.sensee.ui.learningDeck

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset

/**
 * Default values used by the learning cards components.
 *
 * Consumers can use these helpers as-is or derive their own animation specs from them.
 */
public object LearningCardsDefaults {
    public const val FLIP_CAMERA_DISTANCE_MULTIPLIER: Float = 12f

    public fun flipAnimationSpec(): FiniteAnimationSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        )

    public fun snapBackAnimationSpec(): FiniteAnimationSpec<Offset> =
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )

    public fun swipeOutAnimationSpec(): FiniteAnimationSpec<Offset> =
        tween(
            durationMillis = 230,
            easing = FastOutLinearInEasing,
        )

    /**
     * Default animation used for the semantic swipe state when an incomplete drag settles back.
     *
     * This is intentionally non-bouncy so card overlays do not mirror the physical overshoot of
     * the card itself.
     */
    public fun swipeStateSnapBackAnimationSpec(): FiniteAnimationSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )

    /**
     * Default animation used for the semantic swipe state while the card is dismissing.
     *
     * It matches the forward feel of [swipeOutAnimationSpec] without exposing raw offset changes.
     */
    public fun swipeStateDismissAnimationSpec(): FiniteAnimationSpec<Float> =
        tween(
            durationMillis = 230,
            easing = FastOutLinearInEasing,
        )
}
