package app.sensee.core.observability.diagnostics

import app.sensee.core.observability.analytics.AnalyticsTracker
import app.sensee.core.observability.crash.CrashReporter
import app.sensee.core.observability.logging.AppLogger

public class DefaultAppDiagnostics(
    override val logger: AppLogger,
    override val crashReporter: CrashReporter,
    override val analyticsTracker: AnalyticsTracker,
) : AppDiagnostics
