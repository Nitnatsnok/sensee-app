package app.sensee.core.tracing.processors

import android.os.Build
import android.os.Trace
import app.sensee.core.tracing.Span
import app.sensee.core.tracing.SpanData
import app.sensee.core.tracing.SpanProcessor
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@ContributesIntoSet(AppScope::class)
@Inject
public class AndroidSystemTraceSpanProcessor : SpanProcessor {
    override fun onStart(span: Span) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        Trace.beginAsyncSection(span.name.truncate(), span.context.spanId.cookie())
    }

    override fun onEnd(span: SpanData) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        Trace.endAsyncSection(span.name.truncate(), span.context.spanId.cookie())
        for ((key, value) in span.attributes) {
            if (value is Long) {
                Trace.setCounter(key.truncate(), value)
            }
        }
    }

    private fun String.truncate(): String = if (length <= MAX_LABEL_LEN) this else substring(0, MAX_LABEL_LEN)

    private fun String.cookie(): Int = hashCode()

    private companion object {
        // android.os.Trace section labels are limited to 127 USCII characters.
        private const val MAX_LABEL_LEN = 127
    }
}
