package app.sensee.ai.llm.dto

import app.sensee.ai.core.EnrichmentSchema
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
internal data class ChatCompletionRequestDto(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<ChatMessageDto>,
    @SerialName("response_format") val responseFormat: JsonObject,
)

@Serializable
internal data class ChatMessageDto(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String,
)

internal val JsonObjectResponseFormat: JsonObject =
    buildJsonObject { put("type", "json_object") }

/**
 * The de-facto OpenAI-compatible `json_schema` response_format. The schema
 * itself is owned by [EnrichmentSchema] in the seam's `core`; this only wraps
 * it in the provider envelope, so the provider boundary stays the single
 * source of truth (ADR-005).
 */
internal fun jsonSchemaResponseFormat(json: Json): JsonObject =
    buildJsonObject {
        put("type", "json_schema")
        put(
            "json_schema",
            buildJsonObject {
                put("name", "enrichment_response")
                put("schema", json.parseToJsonElement(EnrichmentSchema.jsonSchema))
            },
        )
    }

@Serializable
internal data class ChatCompletionResponseDto(
    @SerialName("choices") val choices: List<ChatChoiceDto> = emptyList(),
)

@Serializable
internal data class ChatChoiceDto(
    @SerialName("message") val message: ChatMessageDto? = null,
)
