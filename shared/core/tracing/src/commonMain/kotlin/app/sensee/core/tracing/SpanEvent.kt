package app.sensee.core.tracing

public data class SpanEvent(
    val name: String,
    val epochNanos: Long,
    val attributes: Map<String, String>,
)
