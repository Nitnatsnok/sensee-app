package app.sensee.ui.designSystem.component.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeTheme

public object SenseeNavigationDefaults {
    public val ItemIconSize: Dp = 24.dp
    public val ActionButtonSize: Dp = 56.dp
    public val ActionButtonIconSize: Dp = 28.dp
    public val DividerThickness: Dp = 1.dp

    /**
     * Upper bound for the expanded navigation rail width. The rail sizes itself to its widest
     * label by default; beyond this cap the label is truncated with an ellipsis to prevent
     * pathological localisations from squeezing the main content.
     */
    public val RailExpandedMaxWidth: Dp = 256.dp

    @Composable
    public fun containerColors(
        container: Color = Color.Unspecified,
        divider: Color = Color.Unspecified,
    ): SenseeNavigationContainerColors {
        val colors = SenseeTheme.colors
        return SenseeNavigationContainerColors(
            container = container.takeOrElse { colors.surfaceContainer },
            divider = divider.takeOrElse { colors.divider },
        )
    }

    @Composable
    public fun itemColors(
        selectedIcon: Color = Color.Unspecified,
        selectedLabel: Color = Color.Unspecified,
        unselectedIcon: Color = Color.Unspecified,
        unselectedLabel: Color = Color.Unspecified,
    ): SenseeNavigationItemColors {
        val colors = SenseeTheme.colors
        return SenseeNavigationItemColors(
            selectedIcon = selectedIcon.takeOrElse { colors.accent },
            selectedLabel = selectedLabel.takeOrElse { colors.accent },
            unselectedIcon = unselectedIcon.takeOrElse { colors.textMuted },
            unselectedLabel = unselectedLabel.takeOrElse { colors.textMuted },
        )
    }
}

@Immutable
public data class SenseeNavigationContainerColors(
    val container: Color,
    val divider: Color,
)

@Immutable
public data class SenseeNavigationItemColors(
    val selectedIcon: Color,
    val selectedLabel: Color,
    val unselectedIcon: Color,
    val unselectedLabel: Color,
)

/**
 * How a [SenseeNavigationItem] should render itself. Provided by the surrounding container via
 * [LocalSenseeNavigationItemLayout] so call sites never thread it manually.
 *
 * - [BottomBar]: icon over a short label (compact, fixed height).
 * - [RailCollapsed]: icon only; the label is exposed to accessibility, not drawn.
 * - [RailExpanded]: icon followed by the full label in a row — long labels get horizontal room.
 */
public enum class SenseeNavigationItemLayout {
    BottomBar,
    RailCollapsed,
    RailExpanded,
}

public val LocalSenseeNavigationItemLayout: ProvidableCompositionLocal<SenseeNavigationItemLayout> =
    compositionLocalOf { SenseeNavigationItemLayout.BottomBar }
