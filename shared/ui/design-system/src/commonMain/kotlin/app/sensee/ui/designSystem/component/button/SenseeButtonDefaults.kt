package app.sensee.ui.designSystem.component.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeTheme

public object SenseeButtonDefaults {
    public val MinHeight: Dp = 48.dp
    public val MinWidth: Dp = 64.dp

    // Default is borderless. Outlined variants must pair their colors with
    // [OutlinedBorderWidth] explicitly — `outlinedColors()` + 0dp renders no visible border.
    public val BorderWidth: Dp = 0.dp
    public val OutlinedBorderWidth: Dp = 1.dp

    /** Size for an inline icon rendered inside the button's content row. */
    public val IconSize: Dp = 20.dp

    // Horizontal touch padding is a button metric, not a layout-rhythm value, so it is
    // sized here rather than on the 4-pt spacing scale (which has no 20.dp step).
    public val HorizontalContentPadding: Dp = 20.dp

    @Composable
    public fun shape(): Shape = SenseeTheme.shapes.large

    @Composable
    public fun contentPadding(): PaddingValues =
        PaddingValues(
            horizontal = HorizontalContentPadding,
            vertical = SenseeTheme.spacing.medium,
        )

    @Composable
    public fun compactContentPadding(): PaddingValues {
        val spacing = SenseeTheme.spacing
        return PaddingValues(
            horizontal = spacing.large,
            vertical = spacing.small,
        )
    }
}
