package app.sensee.core.tracing.processors

import app.sensee.core.observability.logging.AppLogger
import app.sensee.core.tracing.SpanData
import app.sensee.core.tracing.SpanProcessor
import app.sensee.core.tracing.SpanStatus
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@ContributesIntoSet(AppScope::class)
@Inject
public class LoggingSpanProcessor(
    appLogger: AppLogger,
) : SpanProcessor {
    private val logger = appLogger.tag("Tracing")

    override fun onEnd(span: SpanData) {
        logger.info {
            buildString {
                append("span=").append(span.name)
                append(" duration=")
                    .append(span.durationNanos / NANOS_IN_MS_DOUBLE)
                    .append("ms")
                append(" trace=").append(span.context.traceId)
                append(" id=").append(span.context.spanId)
                span.context.parentSpanId?.let { append(" parent=").append(it) }
                if (span.status != SpanStatus.Unset) {
                    append(" status=").append(span.status)
                    span.statusDescription?.let { append(" (").append(it).append(")") }
                }
                if (span.attributes.isNotEmpty()) {
                    append(" attrs=").append(span.attributes)
                }
                if (span.events.isNotEmpty()) {
                    append(" events=").append(span.events.map { it.name })
                }
            }
        }
    }

    private companion object {
        private const val NANOS_IN_MS_DOUBLE: Double = 1_000_000.0
    }
}
