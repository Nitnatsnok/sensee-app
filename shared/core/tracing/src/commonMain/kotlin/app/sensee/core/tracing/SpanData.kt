package app.sensee.core.tracing

public data class SpanData(
    val name: String,
    val context: SpanContext,
    val startEpochNanos: Long,
    val endEpochNanos: Long,
    val durationNanos: Long,
    val attributes: Map<String, Any>,
    val events: List<SpanEvent>,
    val status: SpanStatus,
    val statusDescription: String?,
)
