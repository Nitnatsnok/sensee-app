package app.sensee.core.tracing

public interface SpanProcessor {
    public fun onStart(span: Span) {}

    public fun onEnd(span: SpanData)
}
