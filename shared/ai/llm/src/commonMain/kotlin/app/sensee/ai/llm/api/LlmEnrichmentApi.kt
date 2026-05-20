package app.sensee.ai.llm.api

import app.sensee.ai.llm.dto.ChatCompletionRequestDto
import app.sensee.ai.llm.dto.ChatCompletionResponseDto
import app.sensee.ai.llm.dto.ChatMessageDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject

/**
 * Stateless over endpoint config: the base URL and model arrive per call so the
 * [httpClient] can be a single app-lifetime instance (Ktor best practice).
 */
internal class LlmEnrichmentApi(
    private val httpClient: HttpClient,
) {
    suspend fun complete(
        apiKey: String,
        baseUrl: String,
        model: String,
        messages: List<ChatMessageDto>,
        responseFormat: JsonObject,
    ): ChatCompletionResponseDto {
        val base = baseUrl.trimEnd('/')
        return httpClient
            .post("$base/v1/chat/completions") {
                headers { append(HttpHeaders.Authorization, "Bearer $apiKey") }
                contentType(ContentType.Application.Json)
                setBody(
                    ChatCompletionRequestDto(
                        model = model,
                        messages = messages,
                        responseFormat = responseFormat,
                    ),
                )
            }.body()
    }
}
