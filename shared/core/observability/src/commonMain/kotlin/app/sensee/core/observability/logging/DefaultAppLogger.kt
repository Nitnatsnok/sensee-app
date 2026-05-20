package app.sensee.core.observability.logging

public class DefaultAppLogger(
    private val tags: List<String>,
    private val minSeverity: LogSeverity,
    private val sink: LogSink,
) : AppLogger {
    override fun tag(tag: String): AppLogger =
        DefaultAppLogger(
            tags = tags + tag,
            minSeverity = minSeverity,
            sink = sink,
        )

    override fun verbose(
        throwable: Throwable?,
        message: () -> String,
    ) {
        log(LogSeverity.Verbose, throwable, message)
    }

    override fun debug(
        throwable: Throwable?,
        message: () -> String,
    ) {
        log(LogSeverity.Debug, throwable, message)
    }

    override fun info(
        throwable: Throwable?,
        message: () -> String,
    ) {
        log(LogSeverity.Info, throwable, message)
    }

    override fun warn(
        throwable: Throwable?,
        message: () -> String,
    ) {
        log(LogSeverity.Warn, throwable, message)
    }

    override fun error(
        throwable: Throwable?,
        message: () -> String,
    ) {
        log(LogSeverity.Error, throwable, message)
    }

    private fun log(
        severity: LogSeverity,
        throwable: Throwable?,
        message: () -> String,
    ) {
        if (severity < minSeverity) {
            return
        }

        sink.log(
            LogRecord(
                severity = severity,
                tag = tags.joinToString("."),
                message = message(),
                throwable = throwable,
            ),
        )
    }
}
