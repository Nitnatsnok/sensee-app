package app.sensee.ui.designSystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp

/**
 * Resolved screen-layout metrics: horizontal/vertical padding around screen content, the
 * content max-width cap, and the gap between adjacent panes on wide layouts. The DS does not
 * own a separate width-size-class enum — pick metrics with one of the
 * `sensee[Compact|Medium|Expanded]LayoutMetrics()` factories and provide them via
 * [LocalSenseeAdaptiveLayoutMetrics] from a surrounding adaptive-aware layer
 * (`AppComposeEnvironment`). `SenseeScreenContent` then uses them by default.
 */
public data class SenseeAdaptiveLayoutMetrics(
    val screenHorizontalPadding: Dp,
    val screenVerticalPadding: Dp,
    val contentMaxWidth: Dp,
    val paneGap: Dp,
)

/**
 * Carries the resolved adaptive layout metrics for the current window. `null` means "not
 * provided" — DS primitives fall back to [senseeCompactLayoutMetrics] in that case so they
 * render sensibly without a surrounding adaptive provider (tests, previews, narrow embeddings).
 */
public val LocalSenseeAdaptiveLayoutMetrics: ProvidableCompositionLocal<SenseeAdaptiveLayoutMetrics?> =
    compositionLocalOf { null }

@Composable
public fun senseeCompactLayoutMetrics(): SenseeAdaptiveLayoutMetrics {
    val layout = SenseeTheme.layout
    return SenseeAdaptiveLayoutMetrics(
        screenHorizontalPadding = layout.screenHorizontalPaddingCompact,
        screenVerticalPadding = layout.screenVerticalPaddingCompact,
        contentMaxWidth = layout.contentMaxWidthCompact,
        paneGap = layout.paneGap,
    )
}

@Composable
public fun senseeMediumLayoutMetrics(): SenseeAdaptiveLayoutMetrics {
    val layout = SenseeTheme.layout
    return SenseeAdaptiveLayoutMetrics(
        screenHorizontalPadding = layout.screenHorizontalPaddingMedium,
        screenVerticalPadding = layout.screenVerticalPaddingMedium,
        contentMaxWidth = layout.contentMaxWidthMedium,
        paneGap = layout.paneGap,
    )
}

@Composable
public fun senseeExpandedLayoutMetrics(): SenseeAdaptiveLayoutMetrics {
    val layout = SenseeTheme.layout
    return SenseeAdaptiveLayoutMetrics(
        screenHorizontalPadding = layout.screenHorizontalPaddingExpanded,
        screenVerticalPadding = layout.screenVerticalPaddingExpanded,
        contentMaxWidth = layout.contentMaxWidthExpanded,
        paneGap = layout.paneGap,
    )
}
