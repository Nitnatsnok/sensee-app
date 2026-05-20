package app.sensee.core.tracing

public interface Tracer {
    public fun startSpan(
        name: String,
        parent: SpanContext? = null,
    ): Span
}
