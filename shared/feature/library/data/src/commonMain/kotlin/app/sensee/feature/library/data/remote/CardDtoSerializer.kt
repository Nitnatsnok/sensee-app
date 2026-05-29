package app.sensee.feature.library.data.remote

import app.sensee.ai.core.EnrichmentItemV1
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Splices a card's catalog identity (`id`, `lemma_id`) flat with its
 * ideal-enrichment item: the card IS a projection of its Sense, so the
 * enrichment fields sit directly on the card with no `enrichment` wrapper. In
 * code [CardDto.enrichment] stays a typed [EnrichmentItemV1] reused verbatim
 * from ai.core, so there is still no field duplication — only the wire shape is
 * flat. JSON-only by design (the catalog wire format is always JSON).
 */
internal object CardDtoSerializer : KSerializer<CardDto> {
    private const val ID = "id"
    private const val LEMMA_ID = "lemma_id"

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CardDto")

    override fun deserialize(decoder: Decoder): CardDto {
        val input = decoder as? JsonDecoder ?: error("CardDto can only be read from JSON")
        val obj = input.decodeJsonElement().jsonObject
        val enrichmentObj = JsonObject(obj.filterKeys { it != ID && it != LEMMA_ID })
        return CardDto(
            id = obj.getValue(ID).jsonPrimitive.content,
            lemmaId = obj.getValue(LEMMA_ID).jsonPrimitive.content,
            enrichment = input.json.decodeFromJsonElement(EnrichmentItemV1.serializer(), enrichmentObj),
        )
    }

    override fun serialize(
        encoder: Encoder,
        value: CardDto,
    ) {
        val output = encoder as? JsonEncoder ?: error("CardDto can only be written to JSON")
        val enrichmentObj =
            output.json.encodeToJsonElement(EnrichmentItemV1.serializer(), value.enrichment).jsonObject
        output.encodeJsonElement(
            buildJsonObject {
                put(ID, value.id)
                put(LEMMA_ID, value.lemmaId)
                enrichmentObj.forEach { (key, element) -> put(key, element) }
            },
        )
    }
}
