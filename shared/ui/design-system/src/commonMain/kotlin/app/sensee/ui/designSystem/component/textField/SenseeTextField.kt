package app.sensee.ui.designSystem.component.textField

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.button.SenseeIconButton
import app.sensee.ui.designSystem.component.button.SenseeIconButtonDefaults
import app.sensee.ui.designSystem.icons.Visibility
import app.sensee.ui.designSystem.icons.VisibilityOff
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.TextFieldScope
import com.composeunstyled.TextInput
import com.composeunstyled.UnstyledTextField
import org.jetbrains.compose.resources.stringResource
import sensee.shared.ui.design_system.generated.resources.Res
import sensee.shared.ui.design_system.generated.resources.text_field_reveal_hide
import sensee.shared.ui.design_system.generated.resources.text_field_reveal_show

/**
 * Sensee design-system text field. Wraps `UnstyledTextField` and builds the
 * field container (shape, fill, border, padding) plus label / supporting text /
 * leading / trailing slots around the inner `TextInput`.
 *
 * @param contentPadding horizontal padding insets the whole input row; vertical
 * padding insets only the central text, so leading/trailing controls (e.g. the
 * secure reveal toggle) stay centered within `MinHeight` instead of enlarging the field.
 */
@Composable
public fun SenseeTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    secure: Boolean = false,
    accessibilityLabel: String? = null,
    placeholder: (@Composable () -> Unit)? = null,
    label: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    colors: SenseeTextFieldColors = SenseeTextFieldDefaults.colors(),
    shape: Shape = SenseeTextFieldDefaults.shape(),
    contentPadding: PaddingValues = SenseeTextFieldDefaults.contentPadding(),
    borderWidth: Dp = SenseeTextFieldDefaults.BorderWidth,
    focusedBorderWidth: Dp = SenseeTextFieldDefaults.FocusedBorderWidth,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focused by interactionSource.collectIsFocusedAsState()

    // Secure mode: obscured by default with a built-in reveal toggle, so screens
    // never hand-roll key masking (profile.feature).
    var revealed by remember { mutableStateOf(false) }
    val effectiveOutput =
        if (secure && !revealed) SecureOutputTransformation else outputTransformation
    val effectiveKeyboard =
        if (secure) keyboardOptions.copy(keyboardType = KeyboardType.Password) else keyboardOptions
    val revealToggleLabel =
        if (secure) {
            stringResource(
                if (revealed) Res.string.text_field_reveal_hide else Res.string.text_field_reveal_show,
            )
        } else {
            ""
        }
    val effectiveTrailing: (@Composable () -> Unit)? =
        trailing ?: if (secure) {
            {
                // Icon button centers its content and carries its own touch
                // target, so the toggle stays vertically centered regardless of
                // field height instead of drifting to the top. It takes the
                // field's trailing/disabled colors so caller overrides still
                // apply, but stays neutral on error (an action affordance, not an
                // error indicator), so it skips the error tint.
                SenseeIconButton(
                    onClick = { revealed = !revealed },
                    icon = {
                        SenseeIcon(
                            imageVector = if (revealed) VisibilityOff else Visibility,
                            contentDescription = null,
                        )
                    },
                    enabled = enabled,
                    colors =
                        SenseeIconButtonDefaults.colors(
                            content = colors.trailingIcon,
                            disabledContent = colors.disabledContent,
                        ),
                    accessibilityLabel = revealToggleLabel,
                )
            }
        } else {
            null
        }

    val resolvedStyle =
        colors.resolvedStyle(
            enabled = enabled,
            isError = isError,
            focused = focused,
            borderWidth = borderWidth,
            focusedBorderWidth = focusedBorderWidth,
        )

    UnstyledTextField(
        state = state,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        accessibilityLabel = accessibilityLabel,
        cursorBrush = SolidColor(colors.focusedBorder),
        lineLimits = lineLimits,
        keyboardOptions = effectiveKeyboard,
        inputTransformation = inputTransformation,
        outputTransformation = effectiveOutput,
        interactionSource = interactionSource,
        textColor = resolvedStyle.content,
    ) {
        SenseeTextFieldContent(
            slots =
                SenseeTextFieldSlots(
                    placeholder = placeholder,
                    label = label,
                    supportingText = supportingText,
                    leading = leading,
                    trailing = effectiveTrailing,
                ),
            colors = colors,
            resolvedStyle = resolvedStyle,
            enabled = enabled,
            isError = isError,
            focused = focused,
            shape = shape,
            contentPadding = contentPadding,
        )
    }
}

@Composable
private fun TextFieldScope.SenseeTextFieldContent(
    slots: SenseeTextFieldSlots,
    colors: SenseeTextFieldColors,
    resolvedStyle: SenseeTextFieldResolvedStyle,
    enabled: Boolean,
    isError: Boolean,
    focused: Boolean,
    shape: Shape,
    contentPadding: PaddingValues,
) {
    val spacing = SenseeTheme.spacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        slots.label?.let { label ->
            ProvideContentColor(
                colors.labelColor(
                    enabled = enabled,
                    isError = isError,
                    focused = focused,
                ),
            ) {
                label()
            }
        }

        SenseeTextFieldInputRow(
            slots = slots,
            colors = colors,
            resolvedStyle = resolvedStyle,
            enabled = enabled,
            isError = isError,
            shape = shape,
            contentPadding = contentPadding,
        )

        slots.supportingText?.let { supportingText ->
            ProvideContentColor(
                colors.supportingTextColor(
                    enabled = enabled,
                    isError = isError,
                ),
            ) {
                supportingText()
            }
        }
    }
}

@Composable
private fun TextFieldScope.SenseeTextFieldInputRow(
    slots: SenseeTextFieldSlots,
    colors: SenseeTextFieldColors,
    resolvedStyle: SenseeTextFieldResolvedStyle,
    enabled: Boolean,
    isError: Boolean,
    shape: Shape,
    contentPadding: PaddingValues,
) {
    val spacing = SenseeTheme.spacing
    val layoutDirection = LocalLayoutDirection.current

    // Keep horizontal padding on the row, but move vertical padding onto the
    // input column only. Otherwise a min-touch-target trailing control (the
    // secure reveal toggle is 48dp) stacks on top of the vertical padding and
    // pushes the field past MinHeight; isolating vertical padding lets such a
    // control sit centered within MinHeight while keeping its full tap area.
    val rowPadding =
        PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection),
            end = contentPadding.calculateEndPadding(layoutDirection),
        )
    val inputPadding =
        PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = SenseeTextFieldDefaults.MinHeight)
                .clip(shape)
                .background(resolvedStyle.container)
                .border(
                    width = resolvedStyle.borderWidth,
                    color = resolvedStyle.border,
                    shape = shape,
                ).padding(rowPadding),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        slots.leading?.let { leading ->
            ProvideContentColor(colors.leadingIconColor(enabled = enabled, isError = isError)) {
                leading()
            }
        }

        Box(modifier = Modifier.weight(1f).padding(inputPadding)) {
            TextInput(
                modifier = Modifier.fillMaxWidth(),
                placeholder =
                    slots.placeholder?.let { placeholder ->
                        {
                            ProvideContentColor(colors.placeholderColor(enabled)) {
                                placeholder()
                            }
                        }
                    },
            )
        }

        slots.trailing?.let { trailing ->
            ProvideContentColor(colors.trailingIconColor(enabled = enabled, isError = isError)) {
                trailing()
            }
        }
    }
}

private fun SenseeTextFieldColors.resolvedStyle(
    enabled: Boolean,
    isError: Boolean,
    focused: Boolean,
    borderWidth: Dp,
    focusedBorderWidth: Dp,
): SenseeTextFieldResolvedStyle =
    SenseeTextFieldResolvedStyle(
        container = containerColor(enabled),
        content = contentColor(enabled = enabled, isError = isError),
        border = borderColor(enabled = enabled, focused = focused, isError = isError),
        borderWidth = if (focused || isError) focusedBorderWidth else borderWidth,
    )

private data class SenseeTextFieldSlots(
    val placeholder: (@Composable () -> Unit)?,
    val label: (@Composable () -> Unit)?,
    val supportingText: (@Composable () -> Unit)?,
    val leading: (@Composable () -> Unit)?,
    val trailing: (@Composable () -> Unit)?,
)

private data class SenseeTextFieldResolvedStyle(
    val container: Color,
    val content: Color,
    val border: Color,
    val borderWidth: Dp,
)

/** Renders every character as a bullet so a secret is never shown by default. */
private object SecureOutputTransformation : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        val length = this.length
        if (length > 0) {
            replace(0, length, "•".repeat(length))
        }
    }
}
