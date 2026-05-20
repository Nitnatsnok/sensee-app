package app.sensee.core.observability

import app.sensee.core.observability.analytics.AnalyticsTracker
import app.sensee.core.observability.analytics.NoOpAnalyticsTracker
import app.sensee.core.observability.crash.CrashReporter
import app.sensee.core.observability.crash.platformCrashReporter
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.diagnostics.DefaultAppDiagnostics
import app.sensee.core.observability.logging.AppLogger
import app.sensee.core.observability.logging.DefaultAppLoggerFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
public interface ObservabilityProviders {
    @SingleIn(AppScope::class)
    @Provides
    public fun provideAppLogger(): AppLogger = DefaultAppLoggerFactory().tagged("Sensee")

    @SingleIn(AppScope::class)
    @Provides
    public fun provideCrashReporter(): CrashReporter = platformCrashReporter()

    @SingleIn(AppScope::class)
    @Provides
    public fun provideAnalyticsTracker(): AnalyticsTracker = NoOpAnalyticsTracker

    @SingleIn(AppScope::class)
    @Provides
    public fun provideAppDiagnostics(
        logger: AppLogger,
        crashReporter: CrashReporter,
        analyticsTracker: AnalyticsTracker,
    ): AppDiagnostics =
        DefaultAppDiagnostics(
            logger = logger,
            crashReporter = crashReporter,
            analyticsTracker = analyticsTracker,
        )
}
