package app.sensee.ui.designSystem.component.switch

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeTheme

public object SenseeSwitchDefaults {
    public val TrackWidth: Dp = 52.dp
    public val TrackHeight: Dp = 32.dp
    public val ThumbSize: Dp = 24.dp

    @Composable
    public fun colors(
        checkedTrack: Color = Color.Unspecified,
        uncheckedTrack: Color = Color.Unspecified,
        checkedThumb: Color = Color.Unspecified,
        uncheckedThumb: Color = Color.Unspecified,
    ): SenseeSwitchColors {
        val colors = SenseeTheme.colors
        return SenseeSwitchColors(
            checkedTrack = checkedTrack.takeOrElse { colors.accent },
            uncheckedTrack = uncheckedTrack.takeOrElse { colors.surfaceContainerHighest },
            checkedThumb = checkedThumb.takeOrElse { colors.textOnAccent },
            uncheckedThumb = uncheckedThumb.takeOrElse { colors.textMuted },
        )
    }
}
