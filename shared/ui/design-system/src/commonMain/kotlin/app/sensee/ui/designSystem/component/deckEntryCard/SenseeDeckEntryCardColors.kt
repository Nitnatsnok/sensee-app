package app.sensee.ui.designSystem.component.deckEntryCard

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

@Immutable
public data class SenseeDeckEntryCardColors(
    val container: Color,
    val content: Color,
    val title: Color,
    val subtitle: Color,
    val meta: Color,
    val border: Color,
    val progressTrack: Color,
    val progressIndicator: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
) {
    @Stable
    public fun containerColor(enabled: Boolean): Color = if (enabled) container else disabledContainer

    @Stable
    public fun contentColor(enabled: Boolean): Color = if (enabled) content else disabledContent

    @Stable
    public fun titleColor(enabled: Boolean): Color = if (enabled) title else disabledContent

    @Stable
    public fun subtitleColor(enabled: Boolean): Color = if (enabled) subtitle else disabledContent

    @Stable
    public fun metaColor(enabled: Boolean): Color = if (enabled) meta else disabledContent
}
