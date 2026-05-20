package app.sensee.tts.cache

import app.sensee.core.observability.logging.AppLogger
import app.sensee.core.observability.logging.DefaultAppLoggerFactory
import app.sensee.core.observability.logging.LogRecord
import app.sensee.core.observability.logging.LogSink
import app.sensee.core.observability.logging.NoOpAppLogger
import app.sensee.tts.core.SpeechLocale
import app.sensee.tts.core.SpeechQuality
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.VoiceId
import app.sensee.tts.testKit.FakeSpeechSynthesizer
import app.sensee.tts.testKit.InMemoryAudioClipStore
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CachingSpeechSynthesizerTest {
    @Test
    fun `synthesize caches the clip and serves subsequent calls from the store`() =
        runTest {
            val store = InMemoryAudioClipStore()
            var delegateCalls = 0
            val delegate =
                FakeSpeechSynthesizer(
                    bytesFor = { req ->
                        delegateCalls += 1
                        req.text.encodeToByteArray()
                    },
                )
            val synthesizer = caching(delegate, store, "elevenlabs")
            val request =
                SpeechRequest(
                    text = "hello",
                    locale = SpeechLocale.English,
                    voiceId = VoiceId("rachel"),
                )

            val first = synthesizer.synthesize(request)
            val second = synthesizer.synthesize(request)

            assertEquals(1, delegateCalls, "delegate should only be hit once")
            assertContentEquals(first.bytes, second.bytes)
            assertEquals(1, store.size)
        }

    @Test
    fun `different engine ids produce separate cache entries`() =
        runTest {
            val store = InMemoryAudioClipStore()
            val delegate = FakeSpeechSynthesizer()
            val cachedElevenlabs = caching(delegate, store, "elevenlabs")
            val cachedBackend = caching(delegate, store, "backend")
            val request = SpeechRequest(text = "hello", locale = SpeechLocale.English)

            cachedElevenlabs.synthesize(request)
            cachedBackend.synthesize(request)

            assertEquals(2, store.size)
        }

    @Test
    fun `rate is excluded from the cache key`() =
        runTest {
            val store = InMemoryAudioClipStore()
            val delegate = FakeSpeechSynthesizer()
            val synthesizer = caching(delegate, store, "elevenlabs")
            val baseRequest =
                SpeechRequest(
                    text = "hello",
                    locale = SpeechLocale.English,
                    voiceId = VoiceId("rachel"),
                )

            synthesizer.synthesize(baseRequest)
            synthesizer.synthesize(baseRequest.copy(rate = app.sensee.tts.core.SpeechRate.Slow))

            assertEquals(1, store.size, "rate must not produce a second cache entry")
        }

    @Test
    fun `quality is part of the cache key`() =
        runTest {
            val store = InMemoryAudioClipStore()
            val delegate = FakeSpeechSynthesizer()
            val synthesizer = caching(delegate, store, "elevenlabs")
            val baseRequest =
                SpeechRequest(
                    text = "hello",
                    locale = SpeechLocale.English,
                    voiceId = VoiceId("rachel"),
                    quality = SpeechQuality.Fast,
                )

            synthesizer.synthesize(baseRequest)
            synthesizer.synthesize(baseRequest.copy(quality = SpeechQuality.High))

            assertEquals(2, store.size)
        }

    @Test
    fun `model is part of the cache key`() =
        runTest {
            val store = InMemoryAudioClipStore()
            val delegate = FakeSpeechSynthesizer()
            val synthesizer = caching(delegate, store, "openai")
            val baseRequest = SpeechRequest(text = "hello", locale = SpeechLocale.English)

            synthesizer.synthesize(baseRequest)
            synthesizer.synthesize(baseRequest.copy(modelId = "tts-1"))

            assertEquals(2, store.size)
        }

    @Test
    fun `stream serves cached bytes as a single emission`() =
        runTest {
            val store = InMemoryAudioClipStore()
            val delegate = FakeSpeechSynthesizer()
            val synthesizer = caching(delegate, store, "elevenlabs")
            val request =
                SpeechRequest(
                    text = "hello",
                    locale = SpeechLocale.English,
                    voiceId = VoiceId("rachel"),
                )
            synthesizer.synthesize(request) // populate cache

            val chunks = synthesizer.stream(request).toList()

            assertEquals(1, chunks.size)
            assertContentEquals("hello".encodeToByteArray(), chunks.single())
        }

    @Test
    fun `stream on cache miss persists the full clip then replays from the store`() =
        runTest {
            val store = InMemoryAudioClipStore()
            var streamCalls = 0
            val delegate =
                object : app.sensee.tts.core.SpeechSynthesizer {
                    override suspend fun synthesize(request: SpeechRequest) =
                        error("synthesize must not be called on the streaming path")

                    override fun stream(request: SpeechRequest) =
                        kotlinx.coroutines.flow.flow {
                            streamCalls += 1
                            emit(byteArrayOf(1, 2))
                            emit(byteArrayOf(3))
                        }
                }
            val synthesizer = caching(delegate, store, "openai:test")
            val request = SpeechRequest(text = "hi", locale = SpeechLocale.English)

            val firstChunks = synthesizer.stream(request).toList()
            val secondChunks = synthesizer.stream(request).toList()

            assertEquals(listOf(byteArrayOf(1, 2), byteArrayOf(3)).map { it.toList() }, firstChunks.map { it.toList() })
            assertEquals(1, streamCalls, "second stream must hit the cache, not the delegate")
            assertEquals(1, secondChunks.size, "cache replay is a single chunk")
            assertContentEquals(byteArrayOf(1, 2, 3), secondChunks.single())
            assertEquals(1, store.size)
        }

    @Test
    fun `stream failure does not cache a partial clip`() =
        runTest {
            val store = InMemoryAudioClipStore()
            val delegate =
                object : app.sensee.tts.core.SpeechSynthesizer {
                    override suspend fun synthesize(request: SpeechRequest) = error("unused")

                    override fun stream(request: SpeechRequest) =
                        kotlinx.coroutines.flow.flow {
                            emit(byteArrayOf(1, 2))
                            throw StreamBrokenException()
                        }
                }
            val synthesizer = caching(delegate, store, "openai:test")
            val request = SpeechRequest(text = "hi", locale = SpeechLocale.English)

            assertFailsWith<StreamBrokenException> {
                synthesizer.stream(request).toList()
            }

            assertEquals(0, store.size, "a broken stream must not persist a partial clip")
        }

    @Test
    fun `a store miss delegates and populates the cache`() =
        runTest {
            val store = InMemoryAudioClipStore()
            val delegate = FakeSpeechSynthesizer(bytesFor = { byteArrayOf(1, 2, 3) })
            val synthesizer = caching(delegate, store, "elevenlabs")
            val request =
                SpeechRequest(
                    text = "hi",
                    locale = SpeechLocale.English,
                    voiceId = VoiceId("rachel"),
                )

            synthesizer.synthesize(request)

            val cached = store.snapshot().values.firstOrNull()
            assertNotNull(cached)
            assertContentEquals(byteArrayOf(1, 2, 3), cached.bytes)
        }

    @Test
    fun `synthesize logs a cache miss then a hit`() =
        runTest {
            val sink = RecordingLogSink()
            val store = InMemoryAudioClipStore()
            val synthesizer = caching(FakeSpeechSynthesizer(), store, "elevenlabs", loggerFrom(sink))
            val request = SpeechRequest(text = "hello", locale = SpeechLocale.English)

            synthesizer.synthesize(request)
            synthesizer.synthesize(request)

            val messages = sink.records.map { it.message }
            assertTrue(messages.any { it.contains("cache miss") }, "expected a miss log, got $messages")
            assertTrue(messages.any { it.contains("cache hit") }, "expected a hit log, got $messages")
        }

    private fun caching(
        delegate: app.sensee.tts.core.SpeechSynthesizer,
        store: InMemoryAudioClipStore,
        engineId: String,
        logger: AppLogger = NoOpAppLogger,
    ) = CachingSpeechSynthesizer(delegate, store, engineId, logger)

    private fun loggerFrom(sink: LogSink): AppLogger = DefaultAppLoggerFactory(sink = sink).tagged("test")

    private class RecordingLogSink : LogSink {
        val records = mutableListOf<LogRecord>()

        override fun log(record: LogRecord) {
            records += record
        }
    }

    private class StreamBrokenException : RuntimeException("stream broke")
}
