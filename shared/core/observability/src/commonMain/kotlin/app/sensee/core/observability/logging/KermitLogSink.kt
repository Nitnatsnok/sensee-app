package app.sensee.core.observability.logging

import co.touchlab.kermit.Logger

public class KermitLogSink internal constructor(
    private val logger: Logger,
) : LogSink {
    public constructor() : this(Logger)

    override fun log(record: LogRecord) {
        // Some backends (Android Logcat) truncate a single entry near 4 KB, which
        // cuts off large payloads like LLM request/response bodies. Split long
        // messages so the whole payload survives; the throwable rides the first part.
        val parts = chunkLogMessage(record.message)
        parts.forEachIndexed { index, part ->
            emit(
                severity = record.severity,
                tag = record.tag,
                throwable = if (index == 0) record.throwable else null,
                message = part,
            )
        }
    }

    private fun emit(
        severity: LogSeverity,
        tag: String,
        throwable: Throwable?,
        message: String,
    ) {
        when (severity) {
            LogSeverity.Verbose -> logger.v(throwable = throwable, tag = tag) { message }
            LogSeverity.Debug -> logger.d(throwable = throwable, tag = tag) { message }
            LogSeverity.Info -> logger.i(throwable = throwable, tag = tag) { message }
            LogSeverity.Warn -> logger.w(throwable = throwable, tag = tag) { message }
            LogSeverity.Error -> logger.e(throwable = throwable, tag = tag) { message }
        }
    }
}

// 1800 chars keeps each entry under the ~4 KB Logcat limit even for 2-byte UTF-8
// (Cyrillic) payloads. A short message stays a single un-prefixed entry.
private const val MAX_LOG_CHUNK_CHARS = 1800

internal fun chunkLogMessage(message: String): List<String> {
    if (message.length <= MAX_LOG_CHUNK_CHARS) return listOf(message)
    val parts = message.chunked(MAX_LOG_CHUNK_CHARS)
    return parts.mapIndexed { index, part -> "(${index + 1}/${parts.size}) $part" }
}
