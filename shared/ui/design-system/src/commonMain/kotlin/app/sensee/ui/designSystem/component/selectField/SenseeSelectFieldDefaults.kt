package app.sensee.ui.designSystem.component.selectField

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeTheme

public object SenseeSelectFieldDefaults {
    /** Caps the options popup so a long list scrolls instead of overflowing the window. */
    public val MaxPopupHeight: Dp = 320.dp

    @Composable
    public fun colors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
        placeholder: Color = Color.Unspecified,
        label: Color = Color.Unspecified,
        border: Color = Color.Unspecified,
        chevron: Color = Color.Unspecified,
        popupContainer: Color = Color.Unspecified,
        optionContent: Color = Color.Unspecified,
    ): SenseeSelectFieldColors {
        val colors = SenseeTheme.colors
        return SenseeSelectFieldColors(
            container = container.takeOrElse { colors.surfaceContainerHigh },
            content = content.takeOrElse { colors.textPrimary },
            placeholder = placeholder.takeOrElse { colors.textMuted },
            label = label.takeOrElse { colors.textSecondary },
            border = border.takeOrElse { colors.border },
            chevron = chevron.takeOrElse { colors.textSecondary },
            popupContainer = popupContainer.takeOrElse { colors.surfaceContainerHigh },
            optionContent = optionContent.takeOrElse { colors.textPrimary },
        )
    }
}
