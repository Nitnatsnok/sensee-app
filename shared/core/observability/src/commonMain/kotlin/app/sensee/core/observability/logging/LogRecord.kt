package app.sensee.core.observability.logging

public data class LogRecord(
    val severity: LogSeverity,
    val tag: String,
    val message: String,
    val throwable: Throwable?,
)
