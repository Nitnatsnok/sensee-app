package app.sensee.ui.designSystem.component.checkbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeTheme

public object SenseeCheckboxDefaults {
    public val Size: Dp = 24.dp

    @Composable
    public fun colors(
        checkedTint: Color = Color.Unspecified,
        uncheckedTint: Color = Color.Unspecified,
    ): SenseeCheckboxColors {
        val colors = SenseeTheme.colors
        return SenseeCheckboxColors(
            checkedTint = checkedTint.takeOrElse { colors.accent },
            uncheckedTint = uncheckedTint.takeOrElse { colors.textMuted },
        )
    }
}
