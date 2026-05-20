package app.sensee.core.observability.crash

public object NoOpCrashReporter : CrashReporter {
    override fun recordException(
        throwable: Throwable,
        attributes: Map<String, String>,
    ): Unit = Unit
}
