package app.sensee.core.tracing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext

public suspend fun <T> Tracer.span(
    name: String,
    block: suspend (Span) -> T,
): T {
    val parent = currentCoroutineContext()[SpanCoroutineContext.Key]?.span?.context
    val span = startSpan(name, parent)
    try {
        return withContext(SpanCoroutineContext(span)) { block(span) }
    } catch (cancellation: CancellationException) {
        // Cancellation closes the span (finally below) but is not a span error
        // — it's the caller deciding to stop the traced work, not a failure.
        throw cancellation
    } catch (throwable: Throwable) {
        span.recordException(throwable)
        span.setStatus(SpanStatus.Error, throwable.message)
        throw throwable
    } finally {
        span.end()
    }
}

public suspend fun currentSpan(): Span? = currentCoroutineContext()[SpanCoroutineContext.Key]?.span
