package app.sensee.ui.designSystem.component.pickerField

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeTheme

public object SenseePickerFieldDefaults {
    public val BorderWidth: Dp = 1.dp

    @Composable
    public fun shape(): Shape = SenseeTheme.shapes.medium

    @Composable
    public fun contentPadding(): PaddingValues {
        val spacing = SenseeTheme.spacing
        return PaddingValues(horizontal = spacing.large, vertical = spacing.medium)
    }

    @Composable
    public fun colors(
        container: Color = Color.Unspecified,
        border: Color = Color.Unspecified,
        title: Color = Color.Unspecified,
        value: Color = Color.Unspecified,
        leadingIcon: Color = Color.Unspecified,
        trailingIcon: Color = Color.Unspecified,
        hint: Color = Color.Unspecified,
    ): SenseePickerFieldColors {
        val colors = SenseeTheme.colors
        return SenseePickerFieldColors(
            container = container.takeOrElse { colors.surfaceContainerLow },
            border = border.takeOrElse { colors.border },
            title = title.takeOrElse { colors.textPrimary },
            value = value.takeOrElse { colors.textMuted },
            leadingIcon = leadingIcon.takeOrElse { colors.textMuted },
            trailingIcon = trailingIcon.takeOrElse { colors.textMuted },
            hint = hint.takeOrElse { colors.textMuted },
        )
    }
}
