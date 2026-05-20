package app.sensee.core.tracing

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

public class SpanCoroutineContext(
    public val span: Span,
) : AbstractCoroutineContextElement(Key) {
    public companion object Key : CoroutineContext.Key<SpanCoroutineContext>
}
