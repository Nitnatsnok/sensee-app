package app.sensee.feature.practice.presentation.impl.deck

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import app.sensee.ui.designSystem.component.learningCard.SenseeLearningCardColors
import app.sensee.ui.designSystem.component.learningCard.SenseeLearningCardDefaults
import app.sensee.ui.designSystem.component.learningCard.SenseeLearningCardSide
import app.sensee.ui.learningDeck.FlippableLearningCard
import app.sensee.ui.learningDeck.LearningCardsDefaults

@Composable
public fun SenseeLearningCard(
    isBackVisible: Boolean,
    front: @Composable BoxScope.() -> Unit,
    back: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    colors: SenseeLearningCardColors = SenseeLearningCardDefaults.colors(),
    shape: Shape = SenseeLearningCardDefaults.shape(),
    contentPadding: PaddingValues = SenseeLearningCardDefaults.contentPadding(),
    minHeight: Dp = SenseeLearningCardDefaults.MinHeight,
    borderWidth: Dp = SenseeLearningCardDefaults.BorderWidth,
    animationSpec: FiniteAnimationSpec<Float> = LearningCardsDefaults.flipAnimationSpec(),
    cameraDistanceMultiplier: Float =
        LearningCardsDefaults.FLIP_CAMERA_DISTANCE_MULTIPLIER,
) {
    val clickableModifier =
        if (onClick != null) {
            Modifier.clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
        } else {
            Modifier
        }

    FlippableLearningCard(
        isBackVisible = isBackVisible,
        modifier =
            modifier
                .defaultMinSize(minHeight = minHeight)
                .clip(shape)
                .then(clickableModifier),
        animationSpec = animationSpec,
        cameraDistanceMultiplier = cameraDistanceMultiplier,
        front = {
            SenseeLearningCardSide(
                containerColor =
                    colors.containerColor(
                        enabled = enabled,
                        isBackVisible = false,
                    ),
                contentColor =
                    colors.contentColor(
                        enabled = enabled,
                        isBackVisible = false,
                    ),
                borderColor = colors.border,
                borderWidth = borderWidth,
                shape = shape,
                contentPadding = contentPadding,
                content = front,
            )
        },
        back = {
            SenseeLearningCardSide(
                containerColor =
                    colors.containerColor(
                        enabled = enabled,
                        isBackVisible = true,
                    ),
                contentColor =
                    colors.contentColor(
                        enabled = enabled,
                        isBackVisible = true,
                    ),
                borderColor = colors.border,
                borderWidth = borderWidth,
                shape = shape,
                contentPadding = contentPadding,
                content = back,
            )
        },
    )
}
