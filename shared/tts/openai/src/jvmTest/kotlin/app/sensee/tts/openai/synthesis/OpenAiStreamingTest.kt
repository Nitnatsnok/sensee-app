package app.sensee.tts.openai.synthesis

import app.sensee.tts.core.SpeechLocale
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.TtsError
import app.sensee.tts.core.TtsException
import app.sensee.tts.openai.api.OpenAiSpeechClientFactory
import app.sensee.tts.openai.config.OpenAiSpeechConfig
import app.sensee.tts.openai.config.StaticOpenAiCredentialsProvider
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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// jvmTest + runBlocking (not commonTest + runTest): the client factory installs
// Ktor's HttpTimeout plugin whose real-time delay watcher fights runTest's
// virtual clock — same constraint as ElevenLabsSynthesizerTest.
class OpenAiStreamingTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val audioBytes = byteArrayOf(0x49, 0x44, 0x33, 0x04, 0x10, 0x20)

    private fun synthesizer(engine: MockEngine) =
        OpenAiSynthesizerFactory.create(
            httpClient = OpenAiSpeechClientFactory.create(engine = engine, config = OpenAiSpeechConfig(), json = json),
            config = OpenAiSpeechConfig(),
            credentials = StaticOpenAiCredentialsProvider("test-key"),
        )

    @Test
    fun `stream emits the audio body and targets the speech endpoint`() =
        runBlocking {
            val captured = mutableListOf<String>()
            val engine =
                MockEngine { request ->
                    captured += request.url.toString()
                    respond(
                        content = audioBytes,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType("audio", "mpeg").toString()),
                    )
                }

            val request = SpeechRequest(text = "hello", locale = SpeechLocale.English)
            val chunks = synthesizer(engine).stream(request).toList()

            val joined = chunks.fold(ByteArray(0)) { acc, next -> acc + next }
            assertContentEquals(audioBytes, joined)
            assertTrue(captured.single().endsWith("/v1/audio/speech"))
        }

    @Test
    fun `stream sends the requested model`() =
        runBlocking {
            var body = ""
            val engine =
                MockEngine { request ->
                    body = request.body.toByteArray().decodeToString()
                    respond(
                        content = audioBytes,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType("audio", "mpeg").toString()),
                    )
                }

            val request = SpeechRequest(text = "hello", locale = SpeechLocale.English, modelId = "tts-1")
            synthesizer(engine).stream(request).toList()

            assertTrue(body.contains("\"model\":\"tts-1\""), body)
        }

    @Test
    fun `stream maps a quota response to a quota error`() =
        runBlocking {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = """{"error":"rate limit"}""",
                        status = HttpStatusCode.TooManyRequests,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val request = SpeechRequest(text = "hi", locale = SpeechLocale.English)
            val ex =
                assertFailsWith<TtsException> {
                    synthesizer(engine).stream(request).toList()
                }
            assertTrue(ex.error is TtsError.Quota, "expected Quota but was ${ex.error}")
        }
}
