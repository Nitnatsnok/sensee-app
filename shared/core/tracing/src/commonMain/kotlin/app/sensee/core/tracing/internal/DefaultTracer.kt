package app.sensee.core.tracing.internal

import app.sensee.core.tracing.Span
import app.sensee.core.tracing.SpanContext
import app.sensee.core.tracing.SpanProcessor
import app.sensee.core.tracing.Tracer
import app.sensee.core.tracing.TracingTicker
import kotlin.time.Clock

internal class DefaultTracer(
    private val processors: List<SpanProcessor>,
    private val clock: Clock,
    private val ticker: TracingTicker,
) : Tracer {
    override fun startSpan(
        name: String,
        parent: SpanContext?,
    ): Span {
        val context =
            SpanContext(
                traceId = parent?.traceId ?: randomTraceId(),
                spanId = randomSpanId(),
                parentSpanId = parent?.spanId,
            )
        val span =
            DefaultSpan(
                name = name,
                context = context,
                startEpochNanos = clock.epochNanosNow(),
                startTick = ticker.markNow(),
                processors = processors,
                clock = clock,
            )
        for (processor in processors) processor.onStart(span)
        return span
    }
}
