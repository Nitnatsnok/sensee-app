package app.sensee.ai.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnrichmentSchemaTest {
    @Test
    fun `catalog mirrors the wire item fields in order so the schema cannot drift`() {
        val descriptor = EnrichmentItemV1.serializer().descriptor
        val wireFields = (0 until descriptor.elementsCount).map(descriptor::getElementName)

        assertEquals(wireFields, EnrichmentSchema.fields.map { it.serialName })
    }

    @Test
    fun `json skeleton wraps the item fields in the versioned envelope`() {
        assertEquals(
            "{\"version\":1,\"items\":[{" +
                "\"translation\":\"string\"," +
                "\"surface_form\":\"string\"," +
                "\"unit_type\":\"string\"," +
                "\"base_lemma\":\"string\"," +
                "\"explanation\":\"string\"," +
                "\"examples\":[\"string\"]," +
                "\"preposition_government\":[{\"alternatives\":[\"string\"],\"example\":\"string\"}]," +
                "\"complementation\":[\"string\"]," +
                "\"usage_labels\":[{\"axis\":\"string\",\"value\":\"string\"}]," +
                "\"usage_note\":\"string\"," +
                "\"grammar_tags\":[{\"category\":\"string\",\"form\":\"string\"}]," +
                "\"irregular_forms\":{\"base\":\"string\",\"past\":\"string\",\"past_participle\":\"string\"}" +
                "}]}",
            EnrichmentSchema.jsonSkeleton,
        )
    }

    @Test
    fun `json schema does not force a minimum item count`() {
        // A genuinely monosemous unit must be allowed to return one item;
        // multi-sense coverage is driven by the prompt + retry, not minItems.
        assertTrue(!EnrichmentSchema.jsonSchema.contains("minItems"))
    }

    @Test
    fun `json schema describes the item properties and carries per-field descriptions`() {
        val schema = Json.parseToJsonElement(EnrichmentSchema.jsonSchema).jsonObject

        fun child(
            parent: JsonObject,
            key: String,
        ): JsonObject = parent.getValue(key).jsonObject

        val rootProps = child(schema, "properties")
        val itemSchema = child(child(rootProps, "items"), "items")
        val itemProps = child(itemSchema, "properties")
        val translation = child(itemProps, "translation")

        assertEquals(
            EnrichmentSchema.fields.map { it.serialName }.toSet(),
            itemProps.keys,
        )
        assertTrue(
            translation.getValue("description").toString().contains("native-language equivalent"),
        )
    }
}
