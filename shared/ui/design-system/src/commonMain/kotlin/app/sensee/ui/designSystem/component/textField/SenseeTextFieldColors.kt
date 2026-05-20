package app.sensee.ui.designSystem.component.textField

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

@Immutable
public data class SenseeTextFieldColors(
    val container: Color,
    val content: Color,
    val placeholder: Color,
    val label: Color,
    val supportingText: Color,
    val leadingIcon: Color,
    val trailingIcon: Color,
    val border: Color,
    val focusedBorder: Color,
    val errorContent: Color,
    val errorBorder: Color,
    val errorSupportingText: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
    val disabledBorder: Color,
) {
    @Stable
    public fun containerColor(enabled: Boolean): Color = if (enabled) container else disabledContainer

    @Stable
    public fun contentColor(
        enabled: Boolean,
        isError: Boolean,
    ): Color =
        when {
            !enabled -> disabledContent
            isError -> errorContent
            else -> content
        }

    @Stable
    public fun borderColor(
        enabled: Boolean,
        focused: Boolean,
        isError: Boolean,
    ): Color =
        when {
            !enabled -> disabledBorder
            isError -> errorBorder
            focused -> focusedBorder
            else -> border
        }

    @Stable
    public fun supportingTextColor(
        enabled: Boolean,
        isError: Boolean,
    ): Color =
        when {
            !enabled -> disabledContent
            isError -> errorSupportingText
            else -> supportingText
        }

    @Stable
    public fun labelColor(
        enabled: Boolean,
        isError: Boolean,
        focused: Boolean,
    ): Color =
        when {
            !enabled -> disabledContent
            isError -> errorSupportingText
            focused -> focusedBorder
            else -> label
        }

    @Stable
    public fun placeholderColor(enabled: Boolean): Color = if (enabled) placeholder else disabledContent

    @Stable
    public fun leadingIconColor(
        enabled: Boolean,
        isError: Boolean,
    ): Color =
        when {
            !enabled -> disabledContent
            isError -> errorSupportingText
            else -> leadingIcon
        }

    @Stable
    public fun trailingIconColor(
        enabled: Boolean,
        isError: Boolean,
    ): Color =
        when {
            !enabled -> disabledContent
            isError -> errorSupportingText
            else -> trailingIcon
        }
}
