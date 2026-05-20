package app.sensee.ai.llm.catalog

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Reads the OpenAI-compatible `GET {base}/v1/models` listing. Stateless over
 * endpoint config so the [httpClient] can be a single app-lifetime instance.
 * The HTTP status is inspected explicitly so a rejected key (401/403) is told
 * apart from an unreachable provider — the catalog needs that distinction to
 * report a precise verification reason.
 */
internal class LlmModelsApi(
    private val httpClient: HttpClient,
) {
    suspend fun fetchModelIds(
        apiKey: String,
        baseUrl: String,
    ): ModelsFetch {
        val base = baseUrl.trimEnd('/')
        val response: HttpResponse =
            httpClient.get("$base/v1/models") {
                headers { append(HttpHeaders.Authorization, "Bearer $apiKey") }
            }
        if (!response.status.isSuccess()) {
            return ModelsFetch.Rejected(response.status.value)
        }
        val dto: ModelsResponseDto = response.body()
        return ModelsFetch.Ok(dto.data.mapNotNull { it.id?.takeIf { id -> id.isNotBlank() } })
    }
}

internal sealed interface ModelsFetch {
    data class Ok(
        val ids: List<String>,
    ) : ModelsFetch

    data class Rejected(
        val statusCode: Int,
    ) : ModelsFetch
}

@Serializable
internal data class ModelsResponseDto(
    @SerialName("data") val data: List<ModelDto> = emptyList(),
)

@Serializable
internal data class ModelDto(
    @SerialName("id") val id: String? = null,
)
