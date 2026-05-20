package app.sensee.core.observability.crash

public interface CrashReporter {
    public fun recordException(
        throwable: Throwable,
        attributes: Map<String, String> = emptyMap(),
    )
}
