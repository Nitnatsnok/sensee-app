package app.sensee.core.tracing

public data class SpanContext(
    val traceId: String,
    val spanId: String,
    val parentSpanId: String?,
)
