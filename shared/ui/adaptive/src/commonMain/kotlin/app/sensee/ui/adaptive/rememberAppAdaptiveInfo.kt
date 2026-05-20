package app.sensee.ui.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

@Composable
public fun rememberAppAdaptiveInfo(): AppAdaptiveInfo {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val sizeClass = adaptiveInfo.windowSizeClass

    val widthSizeClass =
        when {
            sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND) ->
                WidthSizeClass.ExtraLarge

            sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) ->
                WidthSizeClass.Large

            sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) ->
                WidthSizeClass.Expanded

            sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
                WidthSizeClass.Medium

            else -> WidthSizeClass.Compact
        }

    val heightSizeClass =
        when {
            sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND) ->
                HeightSizeClass.Expanded

            sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) ->
                HeightSizeClass.Medium

            else -> HeightSizeClass.Compact
        }

    return AppAdaptiveInfo(
        widthSizeClass = widthSizeClass,
        heightSizeClass = heightSizeClass,
    )
}
