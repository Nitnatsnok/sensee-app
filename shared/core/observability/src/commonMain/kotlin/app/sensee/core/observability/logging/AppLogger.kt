package app.sensee.core.observability.logging

public interface AppLogger {
    public fun tag(tag: String): AppLogger

    public fun verbose(
        throwable: Throwable? = null,
        message: () -> String,
    )

    public fun debug(
        throwable: Throwable? = null,
        message: () -> String,
    )

    public fun info(
        throwable: Throwable? = null,
        message: () -> String,
    )

    public fun warn(
        throwable: Throwable? = null,
        message: () -> String,
    )

    public fun error(
        throwable: Throwable? = null,
        message: () -> String,
    )
}
