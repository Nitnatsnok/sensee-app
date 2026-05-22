package app.sensee.appShell.primary

import app.sensee.core.platform.Platform
import app.sensee.core.platform.isWeb
import app.sensee.ui.adaptive.AppAdaptiveInfo

/** Which primary navigation chrome [PrimaryShellScreen] renders. */
internal enum class PrimaryNavLayout {
    TopBar,
    NavigationRail,
    BottomBar,
}

/**
 * Picks the primary navigation chrome from window size and host platform. Web replaces the
 * vertical rail with a horizontal top bar — the native web idiom, and it avoids the wide gap
 * a left-edge rail leaves next to centred content. A compact window keeps the bottom bar on
 * every platform. Platform is a separate axis from [AppAdaptiveInfo], which stays purely
 * about window size.
 */
internal fun selectPrimaryNavLayout(
    platform: Platform,
    adaptiveInfo: AppAdaptiveInfo,
): PrimaryNavLayout =
    when {
        !adaptiveInfo.showNavigationRail -> PrimaryNavLayout.BottomBar
        platform.isWeb -> PrimaryNavLayout.TopBar
        else -> PrimaryNavLayout.NavigationRail
    }
