package app.sensee.ai.llm.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Reads an OpenAI-compatible `POST {base}/v1/embeddings` response. Stateless over
 * endpoint config — base URL, model, and dimension arrive per call — so the
 * [httpClient] can be a single app-lifetime instance (Ktor best practice).
 */
internal class LlmEmbeddingApi(
    private val httpClient: HttpClient,
) {
    suspend fun embed(
        apiKey: String,
        baseUrl: String,
        model: String,
        input: String,
        dimensions: Int,
    ): EmbeddingResponseDto {
        val base = baseUrl.trimEnd('/')
        return httpClient
            .post("$base/v1/embeddings") {
                headers { append(HttpHeaders.Authorization, "Bearer $apiKey") }
                contentType(ContentType.Application.Json)
                setBody(EmbeddingRequestDto(model = model, input = input, dimensions = dimensions))
            }.body()
    }
}

@Serializable
internal data class EmbeddingRequestDto(
    @SerialName("model") val model: String,
    @SerialName("input") val input: String,
    @SerialName("dimensions") val dimensions: Int,
)

@Serializable
internal data class EmbeddingResponseDto(
    @SerialName("data") val data: List<EmbeddingDataDto> = emptyList(),
)

@Serializable
internal data class EmbeddingDataDto(
    @SerialName("embedding") val embedding: List<Float> = emptyList(),
)
