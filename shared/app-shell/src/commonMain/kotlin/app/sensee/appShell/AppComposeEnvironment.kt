package app.sensee.appShell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

@Composable
public fun AppComposeEnvironment(
    themeMode: SenseeThemeMode = SenseeThemeMode.System,
    content: @Composable () -> Unit,
) {
    val adaptiveInfo = rememberAppAdaptiveInfo()
    SenseeTheme(themeMode = themeMode) {
        val layoutMetrics = adaptiveInfo.resolveLayoutMetrics()
        CompositionLocalProvider(
            LocalAdaptiveInfo provides adaptiveInfo,
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
