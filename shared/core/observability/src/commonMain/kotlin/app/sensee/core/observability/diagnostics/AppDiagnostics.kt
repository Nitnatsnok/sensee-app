package app.sensee.core.observability.diagnostics

import app.sensee.core.observability.analytics.AnalyticsTracker
import app.sensee.core.observability.crash.CrashReporter
import app.sensee.core.observability.logging.AppLogger

public interface AppDiagnostics {
    public val logger: AppLogger
    public val crashReporter: CrashReporter
    public val analyticsTracker: AnalyticsTracker
}
