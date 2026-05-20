package app.sensee.ui.designSystem.component.practiceRating

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

@Immutable
public data class SenseePracticeRatingButtonColors(
    val container: Color,
    val content: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
) {
    @Stable
    public fun containerColor(enabled: Boolean): Color = if (enabled) container else disabledContainer

    @Stable
    public fun contentColor(enabled: Boolean): Color = if (enabled) content else disabledContent
}
