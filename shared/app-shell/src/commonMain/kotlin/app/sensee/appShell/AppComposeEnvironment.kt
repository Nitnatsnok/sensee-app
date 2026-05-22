package app.sensee.appShell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.sensee.core.platform.Platform
import app.sensee.ui.adaptive.AppAdaptiveInfo
import app.sensee.ui.adaptive.LocalAdaptiveInfo
import app.sensee.ui.adaptive.WidthSizeClass
import app.sensee.ui.adaptive.rememberAppAdaptiveInfo
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.SenseeThemeMode
import app.sensee.ui.designSystem.theme.senseeCompactLayoutMetrics
import app.sensee.ui.designSystem.theme.senseeExpandedLayoutMetrics
import app.sensee.ui.designSystem.theme.senseeMediumLayoutMetrics

/**
 * Wraps [content] with everything the rest of the UI tree expects: [SenseeTheme],
 * [LocalAdaptiveInfo], [LocalPlatform], and [LocalSenseeAdaptiveLayoutMetrics]. Always call
 * with named arguments — `platform` is required and sits before the defaulted [themeMode],
 * so positional calls become ambiguous if a future caller forgets it.
 */
@Composable
public fun AppComposeEnvironment(
    platform: Platform,
    themeMode: SenseeThemeMode = SenseeThemeMode.System,
    content: @Composable () -> Unit,
) {
    val adaptiveInfo = rememberAppAdaptiveInfo()
    SenseeTheme(themeMode = themeMode) {
        val layoutMetrics = adaptiveInfo.resolveLayoutMetrics()
        CompositionLocalProvider(
            LocalAdaptiveInfo provides adaptiveInfo,
            LocalPlatform provides platform,
            LocalSenseeAdaptiveLayoutMetrics provides layoutMetrics,
        ) {
            content()
        }
    }
}

@Composable
private fun AppAdaptiveInfo.resolveLayoutMetrics(): SenseeAdaptiveLayoutMetrics =
    when {
        isWidthAtLeast(WidthSizeClass.Expanded) -> senseeExpandedLayoutMetrics()
        isWidthAtLeast(WidthSizeClass.Medium) -> senseeMediumLayoutMetrics()
        else -> senseeCompactLayoutMetrics()
    }
