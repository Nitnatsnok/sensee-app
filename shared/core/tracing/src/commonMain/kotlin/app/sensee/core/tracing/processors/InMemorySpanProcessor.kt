package app.sensee.core.tracing.processors

import app.sensee.core.tracing.Span
import app.sensee.core.tracing.SpanData
import app.sensee.core.tracing.SpanProcessor

public class InMemorySpanProcessor : SpanProcessor {
    private val starts = mutableListOf<Span>()
    private val ends = mutableListOf<SpanData>()

    public val startedSpans: List<Span> get() = starts.toList()
    public val finishedSpans: List<SpanData> get() = ends.toList()

    override fun onStart(span: Span) {
        starts.add(span)
    }

    override fun onEnd(span: SpanData) {
        ends.add(span)
    }

    public fun clear() {
        starts.clear()
        ends.clear()
    }
}
