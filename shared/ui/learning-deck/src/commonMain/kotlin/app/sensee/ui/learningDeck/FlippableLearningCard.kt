package app.sensee.ui.learningDeck

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

/**
 * Displays a two-sided card controlled by [state].
 *
 * Use this overload when the visible side should also be controlled from outside the card itself,
 * for example by a toolbar button, keyboard shortcut, or screen-level state holder.
 *
 * When [clickToFlipEnabled] is `true`, clicking the card toggles
 * [LearningCardFlipState.isBackVisible]. When it is `false`, the caller is expected to invoke
 * [LearningCardFlipState.flip], [LearningCardFlipState.showFront], or
 * [LearningCardFlipState.showBack].
 */
@Composable
public fun FlippableLearningCard(
    state: LearningCardFlipState,
    front: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    clickToFlipEnabled: Boolean = true,
    animationSpec: FiniteAnimationSpec<Float> = LearningCardsDefaults.flipAnimationSpec(),
    cameraDistanceMultiplier: Float = LearningCardsDefaults.FLIP_CAMERA_DISTANCE_MULTIPLIER,
    back: @Composable BoxScope.() -> Unit,
) {
    val interactiveModifier =
        if (clickToFlipEnabled) {
            modifier.clickable(onClick = state::flip)
        } else {
            modifier
        }

    FlippableLearningCard(
        isBackVisible = state.isBackVisible,
        modifier = interactiveModifier,
        animationSpec = animationSpec,
        cameraDistanceMultiplier = cameraDistanceMultiplier,
        front = front,
        back = back,
    )
}

/**
 * Displays a two-sided card driven by an external boolean state.
 *
 * This overload is useful when the visible side is already derived from parent state and no
 * dedicated [LearningCardFlipState] is required.
 */
@Composable
public fun FlippableLearningCard(
    isBackVisible: Boolean,
    front: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = LearningCardsDefaults.flipAnimationSpec(),
    cameraDistanceMultiplier: Float = LearningCardsDefaults.FLIP_CAMERA_DISTANCE_MULTIPLIER,
    back: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val rotation =
        animateFloatAsState(
            targetValue = if (isBackVisible) 180f else 0f,
            animationSpec = animationSpec,
            label = "LearningCardFlipRotation",
        )
    // Faces only need the boolean side; deriving it keeps composition invalidations to one
    // per flip (boolean flip at 90deg) instead of one per animation frame.
    val isFrontVisible by remember { derivedStateOf { rotation.value <= 90f } }

    Box(
        modifier =
            modifier.graphicsLayer {
                rotationY = rotation.value
                cameraDistance = cameraDistanceMultiplier.coerceAtLeast(0f) * density.density
            },
        contentAlignment = Alignment.Center,
    ) {
        LearningCardFace(
            isVisible = isFrontVisible,
            content = front,
        )
        LearningCardFace(
            isVisible = !isFrontVisible,
            rotateBy = 180f,
            content = back,
        )
    }
}

@Composable
private fun LearningCardFace(
    isVisible: Boolean,
    rotateBy: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    // The hidden face is edge-on (alpha 0) at the 90deg flip boundary, so not composing
    // its content while hidden is visually identical and halves per-card composition cost
    // across the deck. Faces are static front/back content, so discarding their internal
    // state on each flip is acceptable.
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationY = rotateBy },
    ) {
        if (isVisible) {
            content()
        }
    }
}
