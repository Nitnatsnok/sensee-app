package app.sensee.core.observability.crash

internal actual fun platformCrashReporter(): CrashReporter = NoOpCrashReporter
