package app.sensee.core.observability.logging

/**
 * Discards every record. For tests and other call sites that need an
 * [AppLogger] but no output; production code receives the injected real logger.
 */
public object NoOpAppLogger : AppLogger {
    override fun tag(tag: String): AppLogger = this

    override fun verbose(
        throwable: Throwable?,
        message: () -> String,
    ): Unit = Unit

    override fun debug(
        throwable: Throwable?,
        message: () -> String,
    ): Unit = Unit

    override fun info(
        throwable: Throwable?,
        message: () -> String,
    ): Unit = Unit

    override fun warn(
        throwable: Throwable?,
        message: () -> String,
    ): Unit = Unit

    override fun error(
        throwable: Throwable?,
        message: () -> String,
    ): Unit = Unit
}
