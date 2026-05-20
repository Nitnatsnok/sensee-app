package app.sensee.core.observability.logging

public class DefaultAppLoggerFactory(
    private val sink: LogSink = KermitLogSink(),
    private val minSeverity: LogSeverity = LogSeverity.Debug,
) : AppLoggerFactory {
    override fun tagged(tag: String): AppLogger =
        DefaultAppLogger(
            tags = listOf(tag),
            minSeverity = minSeverity,
            sink = sink,
        )
}
