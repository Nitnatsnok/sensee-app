package app.sensee.core.tracing.internal

import app.sensee.core.tracing.Span
import app.sensee.core.tracing.SpanContext
import app.sensee.core.tracing.SpanData
import app.sensee.core.tracing.SpanEvent
import app.sensee.core.tracing.SpanProcessor
import app.sensee.core.tracing.SpanStatus
import app.sensee.core.tracing.TracingTick
import kotlin.time.Clock

internal class DefaultSpan(
    override val name: String,
    override val context: SpanContext,
    private val startEpochNanos: Long,
    private val startTick: TracingTick,
    private val processors: List<SpanProcessor>,
    private val clock: Clock,
) : Span {
    private val attributes = mutableMapOf<String, Any>()
    private val events = mutableListOf<SpanEvent>()
    private var status: SpanStatus = SpanStatus.Unset
    private var statusDescription: String? = null
    private var ended = false

    override fun setAttribute(
        key: String,
        value: String,
    ) {
        if (ended) return
        attributes[key] = value
    }

    override fun setAttribute(
        key: String,
        value: Long,
    ) {
        if (ended) return
        attributes[key] = value
    }

    override fun setAttribute(
        key: String,
        value: Boolean,
    ) {
        if (ended) return
        attributes[key] = value
    }

    override fun addEvent(
        name: String,
        attributes: Map<String, String>,
    ) {
        if (ended) return
        events.add(SpanEvent(name = name, epochNanos = clock.epochNanosNow(), attributes = attributes))
    }

    override fun setStatus(
        status: SpanStatus,
        description: String?,
    ) {
        if (ended) return
        this.status = status
        this.statusDescription = description
    }

    override fun recordException(throwable: Throwable) {
        if (ended) return
        events.add(
            SpanEvent(
                name = "exception",
                epochNanos = clock.epochNanosNow(),
                attributes =
                    mapOf(
                        "exception.type" to (throwable::class.simpleName ?: "Throwable"),
                        "exception.message" to throwable.message.orEmpty(),
                    ),
            ),
        )
    }

    override fun end() {
        if (ended) return
        ended = true
        val data =
            SpanData(
                name = name,
                context = context,
                startEpochNanos = startEpochNanos,
                endEpochNanos = clock.epochNanosNow(),
                durationNanos = startTick.elapsedNanosNow(),
                attributes = attributes.toMap(),
                events = events.toList(),
                status = status,
                statusDescription = statusDescription,
            )
        for (processor in processors) processor.onEnd(data)
    }
}
