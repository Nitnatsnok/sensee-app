package app.sensee.ai.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiEnrichmentExtensionTest {
    @Test
    fun `cefr extension splices a cefr string field into the item schema`() {
        val props = itemProperties(setOf(CefrEnrichmentExtension))
        val cefr = props.getValue(CefrEnrichmentExtension.KEY).jsonObject

        assertTrue(CefrEnrichmentExtension.KEY in props.keys)
        assertEquals("string", cefr.getValue("type").jsonPrimitive.content)
    }

    @Test
    fun `extraction pulls an owned key into the extensions bucket`() {
        val item = Json.parseToJsonElement("""{"translation":"x","cefr":"B2"}""").jsonObject

        assertEquals(
            mapOf("cefr" to JsonPrimitive("B2")),
            setOf(CefrEnrichmentExtension).extractItemExtensions(item),
        )
    }

    @Test
    fun `an owned key colliding with a built-in is never external`() {
        val rogue =
            object : AiEnrichmentExtension {
                override val id: String = "rogue"
                override val ownedKeys: Set<String> = setOf("translation")

                override fun fields(): List<EnrichmentSchema.Field> = emptyList()
            }

        assertTrue("translation" !in setOf(rogue).externalKeys())
    }

    @Test
    fun `default extensions own only keys disjoint from the built-in wire fields`() {
        val owned = DefaultAiEnrichmentExtensions.flatMapTo(mutableSetOf()) { it.ownedKeys }

        assertTrue(owned.intersect(EnrichmentSchema.builtInFieldNames).isEmpty())
        assertTrue(CefrEnrichmentExtension.KEY in owned)
    }

    private fun itemProperties(extensions: Set<AiEnrichmentExtension>): JsonObject =
        EnrichmentSchema
            .buildJsonSchema(EnrichmentTaxonomy.EMPTY, extensions = extensions)
            .jsonObject
            .getValue("properties")
            .jsonObject
            .getValue("items")
            .jsonObject
            .getValue("items")
            .jsonObject
            .getValue("properties")
            .jsonObject
}
