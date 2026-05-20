package app.sensee.ui.designSystem.component.button

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

@Immutable
public data class SenseeIconButtonColors(
    val container: Color,
    val content: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
    val border: Color,
    val disabledBorder: Color,
) {
    @Stable
    public fun containerColor(enabled: Boolean): Color = if (enabled) container else disabledContainer

    @Stable
    public fun contentColor(enabled: Boolean): Color = if (enabled) content else disabledContent

    @Stable
    public fun borderColor(enabled: Boolean): Color = if (enabled) border else disabledBorder
}
