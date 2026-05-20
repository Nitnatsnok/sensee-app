package app.sensee.core.tracing

import kotlin.time.TimeSource

internal fun interface TracingTicker {
    fun markNow(): TracingTick
}

internal fun interface TracingTick {
    fun elapsedNanosNow(): Long
}

internal object MonotonicTracingTicker : TracingTicker {
    override fun markNow(): TracingTick {
        val mark = TimeSource.Monotonic.markNow()
        return TracingTick { mark.elapsedNow().inWholeNanoseconds }
    }
}
