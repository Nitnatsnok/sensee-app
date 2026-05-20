package app.sensee.core.tracing

import app.sensee.core.tracing.processors.InMemorySpanProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class TracingTest {
    private fun newTracer(
        clock: Clock = MutableClock(epochMillis = 1_000L),
        ticker: TracingTicker = MutableTicker(),
    ): Pair<Tracer, InMemorySpanProcessor> {
        val processor = InMemorySpanProcessor()
        val tracer = Tracing(listOf(processor), clock, ticker).tracer()
        return tracer to processor
    }

    @Test
    fun `span end records SpanData to processor`() =
        runTest {
            val (tracer, processor) = newTracer()

            tracer.span("work") { /* no-op */ }

            assertEquals(1, processor.finishedSpans.size)
            val data = processor.finishedSpans[0]
            assertEquals("work", data.name)
            assertTrue(data.endEpochNanos >= data.startEpochNanos)
            assertTrue(data.durationNanos >= 0L)
            assertEquals(SpanStatus.Unset, data.status)
        }

    @Test
    fun `onStart fires before block body and onEnd after`() =
        runTest {
            val (tracer, processor) = newTracer()

            tracer.span("ordered") {
                assertEquals(1, processor.startedSpans.size)
                assertEquals(0, processor.finishedSpans.size)
            }

            assertEquals(1, processor.finishedSpans.size)
        }

    @Test
    fun `nested span inherits parent traceId and parentSpanId`() =
        runTest {
            val (tracer, processor) = newTracer()

            tracer.span("outer") { outer ->
                tracer.span("inner") { /* no-op */ }
                assertEquals(2, processor.startedSpans.size)
                val outerCtx = outer.context
                val innerCtx = processor.startedSpans[1].context
                assertEquals(outerCtx.traceId, innerCtx.traceId)
                assertEquals(outerCtx.spanId, innerCtx.parentSpanId)
            }
        }

    @Test
    fun `currentSpan returns the active span inside block`() =
        runTest {
            val (tracer, _) = newTracer()

            tracer.span("outer") { outer ->
                val active = currentSpan()
                assertNotNull(active)
                assertEquals(outer.context.spanId, active.context.spanId)
            }
            assertNull(currentSpan())
        }

    @Test
    fun `exception marks span as Error and records event`() =
        runTest {
            val (tracer, processor) = newTracer()

            assertFailsWith<IllegalStateException> {
                tracer.span("failing") {
                    error("boom")
                }
            }

            val data = processor.finishedSpans.single()
            assertEquals(SpanStatus.Error, data.status)
            assertEquals("boom", data.statusDescription)
            val exception = data.events.single { it.name == "exception" }
            assertEquals("IllegalStateException", exception.attributes["exception.type"])
            assertEquals("boom", exception.attributes["exception.message"])
        }

    @Test
    fun `cancellation ends span without Error status`() =
        runTest {
            val (tracer, processor) = newTracer()

            assertFailsWith<CancellationException> {
                tracer.span("cancelled") {
                    throw CancellationException("stop")
                }
            }

            val data = processor.finishedSpans.single()
            assertEquals(SpanStatus.Unset, data.status)
            assertTrue(data.events.none { it.name == "exception" })
        }

    @Test
    fun `attributes and events are captured`() =
        runTest {
            val (tracer, processor) = newTracer()

            tracer.span("decorated") { span ->
                span.setAttribute("user.id", "u-1")
                span.setAttribute("retry.count", 3L)
                span.setAttribute("cache.hit", true)
                span.addEvent("phase", mapOf("step" to "warmup"))
            }

            val data = processor.finishedSpans.single()
            assertEquals("u-1", data.attributes["user.id"])
            assertEquals(3L, data.attributes["retry.count"])
            assertEquals(true, data.attributes["cache.hit"])
            val event = data.events.single()
            assertEquals("phase", event.name)
            assertEquals("warmup", event.attributes["step"])
        }

    @Test
    fun `timestamps come from injected clock and duration comes from ticker`() =
        runTest {
            val clock = MutableClock(epochMillis = 1_000L)
            val ticker = MutableTicker()
            val (tracer, processor) = newTracer(clock, ticker)

            val span = tracer.startSpan("manual")
            clock.epochMillis = 500L
            ticker.elapsedNanos = 250_000_000L
            span.addEvent("phase", emptyMap())
            clock.epochMillis = 900L
            ticker.elapsedNanos = 750_000_000L
            span.end()

            val data = processor.finishedSpans.single()
            assertEquals(1_000_000_000L, data.startEpochNanos)
            assertEquals(500_000_000L, data.events.single().epochNanos)
            assertEquals(900_000_000L, data.endEpochNanos)
            assertEquals(750_000_000L, data.durationNanos)
        }

    @Test
    fun `end is idempotent`() =
        runTest {
            val (tracer, processor) = newTracer()
            val span = tracer.startSpan("manual")

            span.end()
            span.end()

            assertEquals(1, processor.finishedSpans.size)
        }

    private class MutableClock(
        var epochMillis: Long,
    ) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(epochMillis)
    }

    private class MutableTicker : TracingTicker {
        var elapsedNanos: Long = 0L

        override fun markNow(): TracingTick {
            val startNanos = elapsedNanos
            return TracingTick { elapsedNanos - startNanos }
        }
    }
}
