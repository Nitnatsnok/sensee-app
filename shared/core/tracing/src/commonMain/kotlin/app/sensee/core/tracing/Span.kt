package app.sensee.core.tracing

public interface Span {
    public val name: String
    public val context: SpanContext

    public fun setAttribute(
        key: String,
        value: String,
    )

    public fun setAttribute(
        key: String,
        value: Long,
    )

    public fun setAttribute(
        key: String,
        value: Boolean,
    )

    public fun addEvent(
        name: String,
        attributes: Map<String, String> = emptyMap(),
    )

    public fun setStatus(
        status: SpanStatus,
        description: String? = null,
    )

    public fun recordException(throwable: Throwable)

    public fun end()
}
