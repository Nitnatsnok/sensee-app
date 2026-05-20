package app.sensee.core.tracing

import app.sensee.core.tracing.internal.DefaultTracer
import kotlin.time.Clock

public class Tracing(
    private val processors: List<SpanProcessor>,
    private val clock: Clock,
) {
    internal constructor(
        processors: List<SpanProcessor>,
        clock: Clock,
        ticker: TracingTicker,
    ) : this(processors, clock) {
        this.ticker = ticker
    }

    private var ticker: TracingTicker = MonotonicTracingTicker

    public fun tracer(): Tracer =
        DefaultTracer(
            processors = processors,
            clock = clock,
            ticker = ticker,
        )
}
