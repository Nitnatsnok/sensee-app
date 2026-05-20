package app.sensee.core.observability.logging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DefaultAppLoggerTest {
    @Test
    fun `tag creates a new logger and does not mutate the old one`() {
        val sink = RecordingLogSink()
        val logger = logger(sink)
        val taggedLogger = logger.tag("LoginRepository")

        logger.debug { "root" }
        taggedLogger.debug { "scoped" }

        assertEquals("AuthFeature", sink.records[0].tag)
        assertEquals("AuthFeature.LoginRepository", sink.records[1].tag)
    }

    @Test
    fun `tags are joined in order`() {
        val sink = RecordingLogSink()

        logger(sink)
            .tag("LoginRepository")
            .tag("refreshToken")
            .info { "refreshing token" }

        assertEquals("AuthFeature.LoginRepository.refreshToken", sink.records.single().tag)
    }

    @Test
    fun `the message lambda is not evaluated when severity is below the minimum`() {
        val sink = RecordingLogSink()
        var evaluated = false
        val logger =
            logger(
                sink = sink,
                minSeverity = LogSeverity.Info,
            )

        logger.debug {
            evaluated = true
            "debug"
        }

        assertEquals(false, evaluated)
        assertEquals(emptyList(), sink.records)
    }

    @Test
    fun `the message lambda is evaluated when severity is loggable`() {
        val sink = RecordingLogSink()
        var evaluated = false
        val logger =
            logger(
                sink = sink,
                minSeverity = LogSeverity.Info,
            )

        logger.info {
            evaluated = true
            "info"
        }

        assertEquals(true, evaluated)
        assertEquals("info", sink.records.single().message)
    }

    @Test
    fun `the throwable is passed into the log record`() {
        val sink = RecordingLogSink()
        val throwable = IllegalStateException("Failed")

        logger(sink).error(throwable) { "error" }

        assertSame(throwable, sink.records.single().throwable)
    }

    private fun logger(
        sink: RecordingLogSink,
        minSeverity: LogSeverity = LogSeverity.Debug,
    ): AppLogger =
        DefaultAppLoggerFactory(
            sink = sink,
            minSeverity = minSeverity,
        ).tagged("AuthFeature")

    private class RecordingLogSink : LogSink {
        val records = mutableListOf<LogRecord>()

        override fun log(record: LogRecord) {
            records += record
        }
    }
}
