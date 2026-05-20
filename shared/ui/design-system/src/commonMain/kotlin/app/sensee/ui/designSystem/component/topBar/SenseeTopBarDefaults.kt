package app.sensee.ui.designSystem.component.topBar

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeTheme

public object SenseeTopBarDefaults {
    public val Height: Dp = 64.dp
    public val FullscreenHeight: Dp = 56.dp

    public val NavigationIconSize: Dp = 24.dp
    public val ActionIconSize: Dp = 24.dp

    /** Width of the leading navigation slot, sized for a 48dp touch target. */
    public val NavigationSlotWidth: Dp = 48.dp
    public val DividerThickness: Dp = 1.dp

    @Composable
    public fun contentPadding(): PaddingValues {
        val spacing = SenseeTheme.spacing

        return PaddingValues(
            horizontal = spacing.small,
        )
    }

    @Composable
    public fun colors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
        navigationIcon: Color = Color.Unspecified,
        actionIcon: Color = Color.Unspecified,
        divider: Color = Color.Unspecified,
    ): SenseeTopBarColors {
        val colors = SenseeTheme.colors

        return SenseeTopBarColors(
            container = container.takeOrElse { colors.surfaceContainer },
            content = content.takeOrElse { colors.textPrimary },
            navigationIcon = navigationIcon.takeOrElse { colors.textSecondary },
            actionIcon = actionIcon.takeOrElse { colors.textSecondary },
            divider = divider.takeOrElse { colors.divider },
        )
    }

    @Composable
    public fun immersiveColors(
        content: Color = Color.Unspecified,
        navigationIcon: Color = Color.Unspecified,
        actionIcon: Color = Color.Unspecified,
    ): SenseeTopBarColors {
        val colors = SenseeTheme.colors

        return SenseeTopBarColors(
            container = Color.Transparent,
            content = content.takeOrElse { colors.textPrimary },
            navigationIcon = navigationIcon.takeOrElse { colors.textPrimary },
            actionIcon = actionIcon.takeOrElse { colors.textPrimary },
            divider = Color.Transparent,
        )
    }
}
