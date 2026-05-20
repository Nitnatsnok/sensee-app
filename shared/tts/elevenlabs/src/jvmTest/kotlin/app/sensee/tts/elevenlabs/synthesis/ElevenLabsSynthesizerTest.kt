package app.sensee.tts.elevenlabs.synthesis

import app.sensee.core.network.NetworkConfig
import app.sensee.core.network.NetworkHttpClientFactory
import app.sensee.tts.core.AudioFormat
import app.sensee.tts.core.SpeechLocale
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.TtsError
import app.sensee.tts.core.TtsException
import app.sensee.tts.core.VoiceId
import app.sensee.tts.elevenlabs.config.ElevenLabsConfig
import app.sensee.tts.elevenlabs.config.StaticElevenLabsCredentialsProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// Integration test of the ElevenLabs synthesizer over the real client factory against
// MockEngine. jvmTest + runBlocking (not commonTest + runTest) because the factory
// installs Ktor's HttpTimeout plugin, whose real-time `delay` watcher is incompatible
// with runTest's virtual time.
class ElevenLabsSynthesizerTest {
    private val expectedBytes = byteArrayOf(0x49, 0x44, 0x33, 0x04) // ID3 mp3 header

    private val json = Json { ignoreUnknownKeys = true }

    private fun client(engine: MockEngine) =
        NetworkHttpClientFactory.create(
            engine = engine,
            config = NetworkConfig(baseUrl = "https://api.elevenlabs.io/"),
            json = json,
        )

    @Test
    fun `synthesize returns the audio bytes as an mp3 clip`() =
        runBlocking {
            val captured = mutableListOf<String>()
            val engine =
                MockEngine { request ->
                    captured += request.url.toString()
                    respond(
                        content = expectedBytes,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType("audio", "mpeg").toString()),
                    )
                }
            val synthesizer =
                ElevenLabsSynthesizerFactory.create(
                    httpClient = client(engine),
                    config =
                        ElevenLabsConfig(
                            defaultVoiceByLocale = mapOf(SpeechLocale.English to VoiceId("rachel")),
                        ),
                    credentials = StaticElevenLabsCredentialsProvider("test-key"),
                )

            val clip =
                synthesizer.synthesize(
                    SpeechRequest(text = "Hello world", locale = SpeechLocale.English),
                )

            assertEquals(AudioFormat.Mp3, clip.format)
            assertContentEquals(expectedBytes, clip.bytes)
            assertTrue(captured.single().contains("v1/text-to-speech/rachel"))
        }

    @Test
    fun `synthesize sends the requested model id`() =
        runBlocking {
            var body = ""
            val engine =
                MockEngine { request ->
                    body = request.body.toByteArray().decodeToString()
                    respond(
                        content = expectedBytes,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType("audio", "mpeg").toString()),
                    )
                }
            val synthesizer =
                ElevenLabsSynthesizerFactory.create(
                    httpClient = client(engine),
                    config =
                        ElevenLabsConfig(
                            defaultVoiceByLocale = mapOf(SpeechLocale.English to VoiceId("rachel")),
                        ),
                    credentials = StaticElevenLabsCredentialsProvider("test-key"),
                )

            synthesizer.synthesize(
                SpeechRequest(
                    text = "Hello world",
                    locale = SpeechLocale.English,
                    modelId = "eleven_multilingual_v2",
                ),
            )

            assertTrue(body.contains("\"model_id\":\"eleven_multilingual_v2\""), body)
        }

    @Test
    fun `stream emits the audio bytes from the stream endpoint`() =
        runBlocking {
            val captured = mutableListOf<String>()
            val engine =
                MockEngine { request ->
                    captured += request.url.toString()
                    respond(
                        content = expectedBytes,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType("audio", "mpeg").toString()),
                    )
                }
            val synthesizer =
                ElevenLabsSynthesizerFactory.create(
                    httpClient = client(engine),
                    config = ElevenLabsConfig(defaultVoiceByLocale = mapOf(SpeechLocale.English to VoiceId("rachel"))),
                    credentials = StaticElevenLabsCredentialsProvider("test-key"),
                )

            val chunks = synthesizer.stream(SpeechRequest(text = "Hello", locale = SpeechLocale.English)).toList()

            assertContentEquals(expectedBytes, chunks.fold(ByteArray(0)) { acc, next -> acc + next })
            assertTrue(captured.single().contains("v1/text-to-speech/rachel/stream"))
        }

    @Test
    fun `a missing voice for the locale raises a no-voice error`() =
        runBlocking {
            val engine =
                MockEngine { _ ->
                    respond(content = "", status = HttpStatusCode.OK)
                }
            val synthesizer =
                ElevenLabsSynthesizerFactory.create(
                    httpClient = client(engine),
                    config = ElevenLabsConfig(),
                    credentials = StaticElevenLabsCredentialsProvider("test-key"),
                )

            val ex =
                assertFailsWith<TtsException> {
                    synthesizer.synthesize(
                        SpeechRequest(text = "Hola", locale = SpeechLocale.Russian),
                    )
                }
            assertTrue(ex.error is TtsError.NoVoiceForLocale)
        }

    @Test
    fun `a quota-exceeded response is mapped to a quota error`() =
        runBlocking {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = """{"detail":"quota exceeded"}""",
                        status = HttpStatusCode.TooManyRequests,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val synthesizer =
                ElevenLabsSynthesizerFactory.create(
                    httpClient = client(engine),
                    config =
                        ElevenLabsConfig(
                            defaultVoiceByLocale = mapOf(SpeechLocale.English to VoiceId("rachel")),
                        ),
                    credentials = StaticElevenLabsCredentialsProvider("test-key"),
                )

            val ex =
                assertFailsWith<TtsException> {
                    synthesizer.synthesize(
                        SpeechRequest(text = "Hi", locale = SpeechLocale.English),
                    )
                }
            assertTrue(ex.error is TtsError.Quota, "expected Quota but was ${ex.error}")
        }
}
