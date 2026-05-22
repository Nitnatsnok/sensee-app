package app.sensee.appShell

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import app.sensee.core.platform.Platform

/**
 * Ambient host platform, provided by [AppComposeEnvironment]. Lets UI that already keys on
 * window size (`LocalAdaptiveInfo`) additionally branch on platform — kept separate so
 * `AppAdaptiveInfo` stays purely about window size. Static: the platform never changes.
 *
 * Reading this Local outside [AppComposeEnvironment] throws — there is no sensible default
 * (any concrete `Platform` value would silently misclassify on the other hosts). Compose
 * previews and ad-hoc UI tests that mount `PrimaryShellScreen` directly must either wrap
 * in [AppComposeEnvironment] or provide [LocalPlatform] explicitly via
 * `CompositionLocalProvider(LocalPlatform provides …)`.
 */
internal val LocalPlatform: ProvidableCompositionLocal<Platform> =
    staticCompositionLocalOf {
        error(
            "LocalPlatform is not provided. Wrap composition in " +
                "AppComposeEnvironment(platform = …) or provide LocalPlatform manually " +
                "via CompositionLocalProvider for previews and tests.",
        )
    }
