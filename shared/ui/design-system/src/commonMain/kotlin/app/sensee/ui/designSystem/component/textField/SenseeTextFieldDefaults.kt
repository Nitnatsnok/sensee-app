package app.sensee.ui.designSystem.component.textField

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeStateAlphas
import app.sensee.ui.designSystem.theme.SenseeTheme

public object SenseeTextFieldDefaults {
    public val MinHeight: Dp = 56.dp
    public val BorderWidth: Dp = 1.dp
    public val FocusedBorderWidth: Dp = 2.dp

    @Composable
    public fun shape(): Shape = SenseeTheme.shapes.large

    @Composable
    public fun contentPadding(): PaddingValues {
        val spacing = SenseeTheme.spacing
        return PaddingValues(
            horizontal = spacing.large,
            vertical = spacing.medium,
        )
    }

    @Composable
    public fun colors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
        placeholder: Color = Color.Unspecified,
        label: Color = Color.Unspecified,
        supportingText: Color = Color.Unspecified,
        leadingIcon: Color = Color.Unspecified,
        trailingIcon: Color = Color.Unspecified,
        border: Color = Color.Unspecified,
        focusedBorder: Color = Color.Unspecified,
        errorContent: Color = Color.Unspecified,
        errorBorder: Color = Color.Unspecified,
        errorSupportingText: Color = Color.Unspecified,
        disabledContainer: Color = Color.Unspecified,
        disabledContent: Color = Color.Unspecified,
        disabledBorder: Color = Color.Unspecified,
    ): SenseeTextFieldColors {
        val colors = SenseeTheme.colors

        return SenseeTextFieldColors(
            container = container.takeOrElse { colors.surfaceContainerHigh },
            content = content.takeOrElse { colors.textPrimary },
            placeholder = placeholder.takeOrElse { colors.textMuted },
            label = label.takeOrElse { colors.textSecondary },
            supportingText = supportingText.takeOrElse { colors.textSecondary },
            leadingIcon = leadingIcon.takeOrElse { colors.textSecondary },
            trailingIcon = trailingIcon.takeOrElse { colors.textSecondary },
            border = border.takeOrElse { colors.border },
            focusedBorder = focusedBorder.takeOrElse { colors.accent },
            errorContent = errorContent.takeOrElse { colors.textPrimary },
            errorBorder = errorBorder.takeOrElse { colors.danger },
            errorSupportingText = errorSupportingText.takeOrElse { colors.danger },
            disabledContainer =
                disabledContainer.takeOrElse {
                    colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTAINER)
                },
            disabledContent =
                disabledContent.takeOrElse {
                    colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT)
                },
            disabledBorder = disabledBorder.takeOrElse { colors.divider },
        )
    }
}
