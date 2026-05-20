package app.sensee.ui.designSystem.component.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeStateAlphas
import app.sensee.ui.designSystem.theme.SenseeTheme

public object SenseeIconButtonDefaults {
    public val Size: Dp = 40.dp
    public val IconSize: Dp = 24.dp

    // Borderless by default; pass a border color + this width only for outlined icon buttons.
    public val BorderWidth: Dp = 0.dp

    @Composable
    public fun colors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
        disabledContainer: Color = Color.Unspecified,
        disabledContent: Color = Color.Unspecified,
        border: Color = Color.Unspecified,
        disabledBorder: Color = Color.Unspecified,
    ): SenseeIconButtonColors {
        val colors = SenseeTheme.colors

        return SenseeIconButtonColors(
            container = container.takeOrElse { Color.Transparent },
            content = content.takeOrElse { colors.textSecondary },
            disabledContainer = disabledContainer.takeOrElse { Color.Transparent },
            disabledContent =
                disabledContent.takeOrElse {
                    colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT)
                },
            border = border.takeOrElse { Color.Transparent },
            disabledBorder = disabledBorder.takeOrElse { Color.Transparent },
        )
    }

    @Composable
    public fun filledColors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
        disabledContainer: Color = Color.Unspecified,
        disabledContent: Color = Color.Unspecified,
    ): SenseeIconButtonColors {
        val colors = SenseeTheme.colors

        return SenseeIconButtonColors(
            container = container.takeOrElse { colors.accent },
            content = content.takeOrElse { colors.textOnAccent },
            disabledContainer = disabledContainer.takeOrElse { colors.surfaceContainerHigh },
            disabledContent =
                disabledContent.takeOrElse {
                    colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT)
                },
            border = Color.Transparent,
            disabledBorder = Color.Transparent,
        )
    }

    @Composable
    public fun tonalColors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
    ): SenseeIconButtonColors {
        val colors = SenseeTheme.colors

        return SenseeIconButtonColors(
            container = container.takeOrElse { colors.accentContainer },
            content = content.takeOrElse { colors.textOnAccentContainer },
            disabledContainer = colors.surfaceContainerHigh,
            disabledContent = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT),
            border = Color.Transparent,
            disabledBorder = Color.Transparent,
        )
    }
}
