package app.sensee.tts.openai.api

import app.sensee.tts.core.TtsError
import app.sensee.tts.core.TtsException
import app.sensee.tts.openai.config.OpenAiCredentialsProvider
import app.sensee.tts.openai.config.OpenAiSpeechConfig
import app.sensee.tts.openai.dto.SpeechRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val STREAM_CHUNK_SIZE = 8 * 1024

internal class OpenAiSpeechApi(
    private val httpClient: HttpClient,
    private val config: OpenAiSpeechConfig,
    private val credentials: OpenAiCredentialsProvider,
) {
    private val endpoint: String get() = "${config.baseUrl.trimEnd('/')}/v1/audio/speech"

    suspend fun synthesize(body: SpeechRequestDto): ByteArray {
        val apiKey = credentials.apiKey()
        return try {
            httpClient
                .post(endpoint) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $apiKey")
                        append(HttpHeaders.Accept, "audio/mpeg")
                    }
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.body()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            throw throwable.toTtsException()
        }
    }

    /**
     * Streams the audio response body chunk by chunk so playback can start on
     * the first bytes instead of waiting for the whole clip.
     */
    fun stream(body: SpeechRequestDto): Flow<ByteArray> =
        flow {
            val apiKey = credentials.apiKey()
            try {
                httpClient
                    .preparePost(endpoint) {
                        headers {
                            append(HttpHeaders.Authorization, "Bearer $apiKey")
                            append(HttpHeaders.Accept, "audio/mpeg")
                        }
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }.execute { response ->
                        val channel = response.bodyAsChannel()
                        val buffer = ByteArray(STREAM_CHUNK_SIZE)
                        while (true) {
                            val read = channel.readAvailable(buffer)
                            if (read < 0) break
                            if (read > 0) emit(buffer.copyOf(read))
                        }
                    }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                throw throwable.toTtsException()
            }
        }
}

private fun Throwable.toTtsException(): TtsException {
    if (this is TtsException) return this
    val error =
        if (this is ResponseException) {
            val code = response.status.value
            when {
                code == 401 || code == 403 -> TtsError.Network("Authentication failed", code)
                code == 429 -> TtsError.Quota("OpenAI TTS quota exceeded")
                else -> TtsError.Network(message = message, statusCode = code)
            }
        } else {
            TtsError.Network(message = message)
        }
    return TtsException(error, this)
}
