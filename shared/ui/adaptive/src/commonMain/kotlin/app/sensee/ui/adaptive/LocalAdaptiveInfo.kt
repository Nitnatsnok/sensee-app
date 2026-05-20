package app.sensee.ui.adaptive

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

public val LocalAdaptiveInfo: ProvidableCompositionLocal<AppAdaptiveInfo> =
    staticCompositionLocalOf<AppAdaptiveInfo> {
        error("AdaptiveInfo is not provided")
    }
