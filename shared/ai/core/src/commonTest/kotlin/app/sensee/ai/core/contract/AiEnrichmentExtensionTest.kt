package app.sensee.ai.core.contract

import app.sensee.ai.core.model.CefrEnrichmentExtension
import app.sensee.ai.core.model.DefaultAiEnrichmentExtensions
import app.sensee.ai.core.request.EnrichmentSchema
import app.sensee.ai.core.request.EnrichmentTaxonomy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun `the closed item schema admits every registered extension key alongside additionalProperties false`() {
        // I6: built-in item schema sets additionalProperties=false. The closed
        // schema must still admit registered extension keys (e.g. cefr): an
        // extension lives in `properties`, so it is not blocked by the
        // additionalProperties gate. The pair "closed AND extension-aware"
        // is the contract that lets the LLM seam ship structured outputs
        // without dropping live extension fields like `cefr`.
        val item =
            EnrichmentSchema
                .buildJsonSchema(EnrichmentTaxonomy.EMPTY, extensions = setOf(CefrEnrichmentExtension))
                .jsonObject
                .getValue("properties")
                .jsonObject
                .getValue("items")
                .jsonObject
                .getValue("items")
                .jsonObject

        // Closed object: extras are blocked.
        assertFalse(item.getValue("additionalProperties").jsonPrimitive.boolean)
        // But the registered extension key IS in the explicit property list,
        // so an item carrying `cefr` validates against the closed schema.
        val props = item.getValue("properties").jsonObject
        assertTrue(CefrEnrichmentExtension.KEY in props.keys)
        val cefrType =
            props
                .getValue(CefrEnrichmentExtension.KEY)
                .jsonObject
                .getValue("type")
                .jsonPrimitive
                .content
        assertEquals("string", cefrType)
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
