package app.sensee.ui.designSystem.component.deckEntryCard

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeStateAlphas
import app.sensee.ui.designSystem.theme.SenseeTheme

public object SenseeDeckEntryCardDefaults {
    public val MinHeight: Dp = 112.dp
    public val BorderWidth: Dp = 1.dp

    public val LeadingSize: Dp = 48.dp
    public val TrailingSize: Dp = 40.dp
    public val ProgressHeight: Dp = 6.dp

    // 24.dp sits between `shapes.large` (16) and `shapes.extraLarge` (28); kept as a component
    // decision rather than a new global token so the shape scale doesn't fragment.
    public val Shape: Shape = RoundedCornerShape(24.dp)

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
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
        title: Color = Color.Unspecified,
        subtitle: Color = Color.Unspecified,
        meta: Color = Color.Unspecified,
        border: Color = Color.Unspecified,
        progressTrack: Color = Color.Unspecified,
        progressIndicator: Color = Color.Unspecified,
        disabledContainer: Color = Color.Unspecified,
        disabledContent: Color = Color.Unspecified,
    ): SenseeDeckEntryCardColors {
        val colors = SenseeTheme.colors

        return SenseeDeckEntryCardColors(
            container = container.takeOrElse { colors.surfaceContainerLow },
            content = content.takeOrElse { colors.textPrimary },
            title = title.takeOrElse { colors.textPrimary },
            subtitle = subtitle.takeOrElse { colors.textSecondary },
            meta = meta.takeOrElse { colors.textMuted },
            border = border.takeOrElse { colors.divider },
            progressTrack = progressTrack.takeOrElse { colors.surfaceContainerLow },
            progressIndicator = progressIndicator.takeOrElse { colors.accent },
            disabledContainer = disabledContainer.takeOrElse { colors.surfaceContainerLow },
            disabledContent =
                disabledContent.takeOrElse {
                    colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT)
                },
        )
    }
}
