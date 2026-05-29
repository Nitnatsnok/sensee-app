package app.sensee.tts.elevenlabs.api

import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.tts.core.TtsError
import app.sensee.tts.core.TtsException
import app.sensee.tts.elevenlabs.config.ElevenLabsConfig
import app.sensee.tts.elevenlabs.config.ElevenLabsCredentialsProvider
import app.sensee.tts.elevenlabs.dto.RemoteModelDto
import app.sensee.tts.elevenlabs.dto.TextToSpeechRequestDto
import app.sensee.tts.elevenlabs.dto.VoiceListResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.IOException

private const val STREAM_CHUNK_SIZE = 8 * 1024

internal class ElevenLabsApi(
    private val httpClient: HttpClient,
    private val config: ElevenLabsConfig,
    private val credentials: ElevenLabsCredentialsProvider,
) {
    suspend fun synthesize(
        voiceId: String,
        body: TextToSpeechRequestDto,
    ): ByteArray {
        val apiKey = credentials.apiKey()
        return wrapErrors {
            httpClient
                .post(buildUrl("v1/text-to-speech/$voiceId")) {
                    applyAuth(apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(body)
                    queryFormat()
                }.body()
        }
    }

    /**
     * Streams the audio response chunk by chunk from the dedicated `/stream`
     * endpoint so playback can start on the first bytes instead of waiting for
     * the whole clip.
     */
    fun stream(
        voiceId: String,
        body: TextToSpeechRequestDto,
    ): Flow<ByteArray> =
        flow {
            val apiKey = credentials.apiKey()
            wrapErrors {
                httpClient
                    .preparePost(buildUrl("v1/text-to-speech/$voiceId/stream")) {
                        applyAuth(apiKey)
                        contentType(ContentType.Application.Json)
                        setBody(body)
                        queryFormat()
                    }.execute { response ->
                        val channel = response.bodyAsChannel()
                        val buffer = ByteArray(STREAM_CHUNK_SIZE)
                        while (true) {
                            val read = channel.readAvailable(buffer)
                            if (read < 0) break
                            if (read > 0) emit(buffer.copyOf(read))
                        }
                    }
            }
        }

    suspend fun voices(): VoiceListResponseDto {
        val apiKey = credentials.apiKey()
        return wrapErrors {
            httpClient.get(buildUrl("v1/voices")) { applyAuth(apiKey) }.body()
        }
    }

    suspend fun models(): List<RemoteModelDto> {
        val apiKey = credentials.apiKey()
        return wrapErrors {
            httpClient.get(buildUrl("v1/models")) { applyAuth(apiKey) }.body()
        }
    }

    private fun buildUrl(path: String): String {
        val base = config.baseUrl.trimEnd('/')
        return "$base/${path.trimStart('/')}"
    }

    private fun HttpRequestBuilder.applyAuth(apiKey: String) {
        headers {
            append("xi-api-key", apiKey)
            append(HttpHeaders.Accept, "audio/mpeg")
        }
    }

    private fun HttpRequestBuilder.queryFormat() {
        url.parameters.append("output_format", config.outputFormat)
    }
}

private fun Throwable.toTtsException(): TtsException {
    val error =
        when (this) {
            is TtsException -> error
            is HttpRequestTimeoutException -> TtsError.Network(message = message)
            is ClientRequestException -> classifyResponseException(this)
            is ServerResponseException -> classifyResponseException(this)
            is ResponseException -> classifyResponseException(this)
            is IOException -> TtsError.Network(message = message)
            else -> TtsError.Unknown(message = message)
        }
    return TtsException(error, this)
}

private fun classifyResponseException(exception: ResponseException): TtsError {
    val code = exception.response.status.value
    return when {
        code == 401 || code == 403 -> TtsError.Network("Authentication failed", code)
        code == 429 -> TtsError.Quota("ElevenLabs quota exceeded")
        else -> TtsError.Network(message = exception.message, statusCode = code)
    }
}

private suspend inline fun <T> wrapErrors(crossinline block: suspend () -> T): T =
    runCatchingCancellable {
        block()
    }.getOrElse { throwable ->
        throw throwable.toTtsException()
    }
