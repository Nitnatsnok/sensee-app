package app.sensee.core.observability.logging

import co.touchlab.kermit.Logger

public class KermitLogSink internal constructor(
    private val logger: Logger,
) : LogSink {
    public constructor() : this(Logger)

    override fun log(record: LogRecord) {
        when (record.severity) {
            LogSeverity.Verbose ->
                logger.v(
                    throwable = record.throwable,
                    tag = record.tag,
                ) {
                    record.message
                }
            LogSeverity.Debug ->
                logger.d(
                    throwable = record.throwable,
                    tag = record.tag,
                ) {
                    record.message
                }
            LogSeverity.Info ->
                logger.i(
                    throwable = record.throwable,
                    tag = record.tag,
                ) {
                    record.message
                }
            LogSeverity.Warn ->
                logger.w(
                    throwable = record.throwable,
                    tag = record.tag,
                ) {
                    record.message
                }
            LogSeverity.Error ->
                logger.e(
                    throwable = record.throwable,
                    tag = record.tag,
                ) {
                    record.message
                }
        }
    }
}
