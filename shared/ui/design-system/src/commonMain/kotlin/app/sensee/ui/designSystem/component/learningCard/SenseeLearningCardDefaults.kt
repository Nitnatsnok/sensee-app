package app.sensee.ui.designSystem.component.learningCard

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeTheme

public object SenseeLearningCardDefaults {
    public val MinHeight: Dp = 320.dp
    public val BorderWidth: Dp = 1.dp

    @Composable
    public fun shape(): Shape = SenseeTheme.shapes.extraLarge

    @Composable
    public fun contentPadding(): PaddingValues {
        val spacing = SenseeTheme.spacing

        return PaddingValues(
            horizontal = spacing.large,
            vertical = spacing.large,
        )
    }

    @Composable
    public fun colors(
        frontContainer: Color = Color.Unspecified,
        frontContent: Color = Color.Unspecified,
        backContainer: Color = Color.Unspecified,
        backContent: Color = Color.Unspecified,
        border: Color = Color.Unspecified,
        disabledContainer: Color = Color.Unspecified,
        disabledContent: Color = Color.Unspecified,
    ): SenseeLearningCardColors {
        val colors = SenseeTheme.colors

        return SenseeLearningCardColors(
            frontContainer = frontContainer.takeOrElse { colors.surfaceContainerLow },
            frontContent = frontContent.takeOrElse { colors.textPrimary },
            backContainer = backContainer.takeOrElse { colors.surfaceContainerHighest },
            backContent = backContent.takeOrElse { colors.textPrimary },
            border = border.takeOrElse { colors.divider },
            disabledContainer = disabledContainer.takeOrElse { colors.surfaceContainerLow },
            disabledContent = disabledContent.takeOrElse { colors.textMuted },
        )
    }
}
