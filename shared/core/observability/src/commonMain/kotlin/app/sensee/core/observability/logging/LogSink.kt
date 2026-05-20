package app.sensee.core.observability.logging

public interface LogSink {
    public fun log(record: LogRecord)
}
