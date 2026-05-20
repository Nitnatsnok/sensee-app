package app.sensee.ui.designSystem.component.textField

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.Text
import com.composeunstyled.TextFieldScope
import com.composeunstyled.TextInput
import com.composeunstyled.UnstyledTextField
import com.composeunstyled.minimumInteractiveComponentSize
import org.jetbrains.compose.resources.stringResource
import sensee.shared.ui.design_system.generated.resources.Res
import sensee.shared.ui.design_system.generated.resources.text_field_reveal_hide
import sensee.shared.ui.design_system.generated.resources.text_field_reveal_show

/**
 * Sensee design-system text field. Wraps `UnstyledTextField` and builds the
 * field container (shape, fill, border, padding) plus label / supporting text /
 * leading / trailing slots around the inner `TextInput`.
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
                Text(
                    text = revealToggleLabel,
                    modifier =
                        Modifier
                            .minimumInteractiveComponentSize()
                            .clickable(
                                enabled = enabled,
                                role = Role.Button,
                            ) { revealed = !revealed },
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

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .defaultMinSize(minHeight = SenseeTextFieldDefaults.MinHeight)
                .clip(shape)
                .background(resolvedStyle.container)
                .border(
                    width = resolvedStyle.borderWidth,
                    color = resolvedStyle.border,
                    shape = shape,
                ).padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        slots.leading?.let { leading ->
            ProvideContentColor(colors.leadingIconColor(enabled = enabled, isError = isError)) {
                leading()
            }
        }

        Box(modifier = Modifier.weight(1f)) {
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
