package app.sensee.ui.designSystem.component.learningCard

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public data class SenseeLearningCardColors(
    val frontContainer: Color,
    val frontContent: Color,
    val backContainer: Color,
    val backContent: Color,
    val border: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
) {
    public fun containerColor(
        enabled: Boolean,
        isBackVisible: Boolean,
    ): Color =
        when {
            !enabled -> disabledContainer
            isBackVisible -> backContainer
            else -> frontContainer
        }

    public fun contentColor(
        enabled: Boolean,
        isBackVisible: Boolean,
    ): Color =
        when {
            !enabled -> disabledContent
            isBackVisible -> backContent
            else -> frontContent
        }
}
