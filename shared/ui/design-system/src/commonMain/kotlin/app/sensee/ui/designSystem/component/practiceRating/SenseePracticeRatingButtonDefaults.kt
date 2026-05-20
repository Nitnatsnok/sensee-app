package app.sensee.ui.designSystem.component.practiceRating

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeStateAlphas
import app.sensee.ui.designSystem.theme.SenseeTheme

public object SenseePracticeRatingButtonDefaults {
    public val MinWidth: Dp = 56.dp
    public val MinHeight: Dp = 80.dp
    public val IconSize: Dp = 24.dp
    public val DirectionIconSize: Dp = 16.dp

    // 20.dp sits between `shapes.large` (16) and `shapes.extraLarge` (28); kept as a component
    // decision rather than a new global token so the shape scale doesn't fragment.
    public val Shape: Shape = RoundedCornerShape(20.dp)

    // Rating buttons sit on saturated containers, so a slightly higher disabled alpha than the
    // generic `SenseeStateAlphas.DISABLED_CONTAINER` (0.12) keeps the muted state legible.
    private const val DISABLED_CONTAINER_ALPHA = 0.18f

    @Composable
    public fun againColors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
    ): SenseePracticeRatingButtonColors {
        val colors = SenseeTheme.colors
        return SenseePracticeRatingButtonColors(
            container = container.takeOrElse { colors.danger },
            content = content.takeOrElse { colors.textOnDanger },
            disabledContainer = colors.textMuted.copy(alpha = DISABLED_CONTAINER_ALPHA),
            disabledContent = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT),
        )
    }

    @Composable
    public fun hardColors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
    ): SenseePracticeRatingButtonColors {
        val colors = SenseeTheme.colors
        return SenseePracticeRatingButtonColors(
            container = container.takeOrElse { colors.warning },
            content = content.takeOrElse { colors.textOnWarning },
            disabledContainer = colors.textMuted.copy(alpha = DISABLED_CONTAINER_ALPHA),
            disabledContent = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT),
        )
    }

    @Composable
    public fun goodColors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
    ): SenseePracticeRatingButtonColors {
        val colors = SenseeTheme.colors
        return SenseePracticeRatingButtonColors(
            container = container.takeOrElse { colors.success },
            content = content.takeOrElse { colors.textOnSuccess },
            disabledContainer = colors.textMuted.copy(alpha = DISABLED_CONTAINER_ALPHA),
            disabledContent = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT),
        )
    }

    @Composable
    public fun easyColors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
    ): SenseePracticeRatingButtonColors {
        val colors = SenseeTheme.colors
        return SenseePracticeRatingButtonColors(
            container = container.takeOrElse { colors.info },
            content = content.takeOrElse { colors.textOnInfo },
            disabledContainer = colors.textMuted.copy(alpha = DISABLED_CONTAINER_ALPHA),
            disabledContent = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT),
        )
    }
}
