package app.sensee.ai.curatedEnrichment

import app.sensee.ai.core.AiEnrichmentClient
import app.sensee.ai.core.AiEnrichmentExtension
import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentItemV1
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.ai.core.EnrichmentResponseMapper
import app.sensee.ai.core.EnrichmentResponseV1
import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.core.extractItemExtensions
import app.sensee.core.coroutines.runCatchingCancellable
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Curated [AiEnrichmentClient] served by the (mock) backend over HTTP:
 * `GET enrichment/{lemma-slug}` returns a wire [EnrichmentResponseV1] for a
 * covered lemma, `404` for a miss. A miss returns an empty available result so
 * the router can fall through to the LLM; transport/decode failures degrade.
 */
@Inject
public class CuratedAiEnrichmentClient(
    private val httpClient: HttpClient,
    private val extensions: Set<AiEnrichmentExtension>,
) : AiEnrichmentClient {
    override suspend fun enrich(request: EnrichmentRequest): EnrichmentResult {
        val term = request.term.trim().lowercase()
        if (term.isEmpty()) {
            return EMPTY_AVAILABLE
        }
        val items =
            when (val lookup = fetchItems(term)) {
                is ItemLookup.Found -> {
                    if (lookup.version != EnrichmentResponseV1.SCHEMA_VERSION) {
                        return EnrichmentResult(
                            availability =
                                EnrichmentAvailability.Degraded(
                                    "unsupported curated enrichment schema version ${lookup.version} for '$term'",
                                ),
                            suggestions = emptyList(),
                        )
                    }
                    lookup.items
                }
                ItemLookup.Miss -> return EMPTY_AVAILABLE
                is ItemLookup.Degraded ->
                    return EnrichmentResult(
                        availability = EnrichmentAvailability.Degraded(lookup.reason),
                        suggestions = emptyList(),
                    )
            }
        return decodeItems(items).toResult(term)
    }

    private suspend fun fetchItems(term: String): ItemLookup =
        runCatchingCancellable {
            val body =
                httpClient
                    .get {
                        url {
                            appendPathSegments(ENDPOINT_PREFIX, term.toSlug(), encodeSlash = true)
                        }
                    }.bodyAsText()
            val root = JSON.parseToJsonElement(body) as? JsonObject
            if (root == null) {
                ItemLookup.Degraded("curated enrichment response for '$term' was not a JSON object")
            } else {
                val versionPrimitive = root["version"] as? JsonPrimitive
                val version =
                    versionPrimitive
                        ?.takeUnless { it.isString }
                        ?.content
                        ?.toIntOrNull()
                        ?: return ItemLookup.Degraded(
                            "curated enrichment response for '$term' had no integer schema version",
                        )
                val items =
                    root["items"] as? JsonArray
                        ?: return ItemLookup.Degraded(
                            "curated enrichment response for '$term' had no items array",
                        )
                if (items.isEmpty()) {
                    return ItemLookup.Degraded("curated enrichment response for '$term' had no items")
                }
                ItemLookup.Found(version = version, items = items)
            }
        }.getOrElse { throwable ->
            when (throwable) {
                is ClientRequestException -> {
                    // 404 is the curated miss; any other 4xx is an unexpected backend state.
                    if (throwable.response.status == HttpStatusCode.NotFound) {
                        ItemLookup.Miss
                    } else {
                        ItemLookup.Degraded("curated enrichment HTTP ${throwable.response.status.value} for '$term'")
                    }
                }
                else ->
                    ItemLookup.Degraded(
                        "curated enrichment unavailable: ${throwable.message ?: throwable::class.simpleName}",
                    )
            }
        }

    private fun decodeItems(items: JsonArray): DecodedItems {
        val rawItems = mutableListOf<EnrichmentItemV1>()
        val itemExtensions = mutableListOf<Map<String, JsonElement>>()
        var malformedItems = 0
        for (element in items) {
            val decoded = decodeElement(element)
            if (decoded == null) {
                malformedItems++
            } else {
                rawItems += decoded.item
                itemExtensions += decoded.extensions
            }
        }
        return DecodedItems(rawItems, itemExtensions, malformedItems)
    }

    private fun DecodedItems.toResult(term: String): EnrichmentResult {
        if (rawItems.isEmpty()) {
            return EnrichmentResult(
                availability =
                    EnrichmentAvailability.Degraded(
                        "curated enrichment item decode failed for '$term'",
                    ),
                suggestions = emptyList(),
            )
        }

        val response = EnrichmentResponseV1(items = rawItems)
        val mapped = EnrichmentResponseMapper.map(response, itemExtensions)
        if (malformedItems == 0) {
            return mapped
        }
        val reason = "curated enrichment skipped $malformedItems malformed item(s) for '$term'"
        val availability =
            when (val mappedAvailability = mapped.availability) {
                EnrichmentAvailability.Available -> EnrichmentAvailability.Degraded(reason)
                is EnrichmentAvailability.Degraded ->
                    EnrichmentAvailability.Degraded("${mappedAvailability.reason}; $reason")
                is EnrichmentAvailability.Unavailable -> mappedAvailability
            }
        return mapped.copy(availability = availability)
    }

    private fun decodeElement(element: JsonElement): DecodedItem? = (element as? JsonObject)?.let(::decodeItem)

    private fun decodeItem(obj: JsonObject): DecodedItem? {
        // kotlinx-serialization throws several exception types for shape mismatch; bad items are dropped.
        return runCatchingCancellable {
            val item = JSON.decodeFromJsonElement(EnrichmentItemV1.serializer(), obj)
            DecodedItem(item, extensions.extractItemExtensions(obj))
        }.getOrElse { throwable ->
            when (throwable) {
                is SerializationFailure,
                is IllegalArgumentException,
                is IllegalStateException,
                -> null
                else -> throw throwable
            }
        }
    }

    private data class DecodedItem(
        val item: EnrichmentItemV1,
        val extensions: Map<String, JsonElement>,
    )

    private data class DecodedItems(
        val rawItems: List<EnrichmentItemV1>,
        val itemExtensions: List<Map<String, JsonElement>>,
        val malformedItems: Int,
    )

    private sealed interface ItemLookup {
        data class Found(
            val version: Int,
            val items: JsonArray,
        ) : ItemLookup

        data object Miss : ItemLookup

        data class Degraded(
            val reason: String,
        ) : ItemLookup
    }

    private companion object {
        const val ENDPOINT_PREFIX: String = "enrichment"

        // Lenient + ignoreUnknownKeys to keep decoding forward-compatible.
        val JSON: Json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

        val EMPTY_AVAILABLE: EnrichmentResult =
            EnrichmentResult(EnrichmentAvailability.Available, emptyList())

        /** Lemma → path segment: a multi-word lemma ("come across") slugs to "come-across". */
        fun String.toSlug(): String = trim().lowercase().replace(' ', '-')
    }
}

private typealias SerializationFailure = kotlinx.serialization.SerializationException
