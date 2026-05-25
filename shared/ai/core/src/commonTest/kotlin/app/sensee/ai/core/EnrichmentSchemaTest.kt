package app.sensee.ai.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
    fun `built json schema does not force a minimum item count`() {
        val schema = EnrichmentSchema.buildJsonSchema(emptySet(), emptySet(), emptyMap(), emptyMap())

        assertTrue(!schema.toString().contains("minItems"))
    }

    @Test
    fun `built json schema describes the item properties and carries per-field descriptions`() {
        val schema = buildEmpty()

        val itemProps = itemProperties(schema)

        assertEquals(EnrichmentSchema.fields.map { it.serialName }.toSet(), itemProps.keys)
        val translation = itemProps.obj("translation")
        assertTrue(translation.str("description").contains("native-language equivalent"))
    }

    @Test
    fun `unit_type carries an enum drawn from the supplied unit type ids`() {
        val schema =
            EnrichmentSchema
                .buildJsonSchema(
                    unitTypeIds = setOf("verb", "noun", "adjective"),
                    complementIds = emptySet(),
                    usageAxesAndValues = emptyMap(),
                    grammarCategoriesAndForms = emptyMap(),
                ).jsonObject

        val unitType = itemProperties(schema).obj("unit_type")

        assertEquals("string", unitType.str("type"))
        assertEquals(listOf("adjective", "noun", "verb"), unitType.arr("enum").asContentList())
    }

    @Test
    fun `complementation array items carry an enum drawn from the supplied complement ids`() {
        val schema =
            EnrichmentSchema
                .buildJsonSchema(
                    unitTypeIds = emptySet(),
                    complementIds = setOf("noun", "gerund", "to_infinitive"),
                    usageAxesAndValues = emptyMap(),
                    grammarCategoriesAndForms = emptyMap(),
                ).jsonObject

        val items = itemProperties(schema).obj("complementation").obj("items")

        assertEquals(listOf("gerund", "noun", "to_infinitive"), items.arr("enum").asContentList())
    }

    @Test
    fun `usage_labels items are a oneOf per axis with const axis and enum values`() {
        val schema =
            EnrichmentSchema
                .buildJsonSchema(
                    unitTypeIds = emptySet(),
                    complementIds = emptySet(),
                    usageAxesAndValues =
                        mapOf(
                            "register" to setOf("formal", "informal"),
                            "region" to setOf("bre", "ame"),
                        ),
                    grammarCategoriesAndForms = emptyMap(),
                ).jsonObject

        val usageItems = itemProperties(schema).obj("usage_labels").obj("items")
        val byAxis = usageItems.arr("oneOf").associateBy { branchConst(it.jsonObject, key = "axis") }

        assertEquals(setOf("region", "register"), byAxis.keys)
        assertEquals(
            listOf("formal", "informal"),
            branchEnum(byAxis.getValue("register").jsonObject, valueKey = "value"),
        )
        assertEquals(
            listOf("ame", "bre"),
            branchEnum(byAxis.getValue("region").jsonObject, valueKey = "value"),
        )
    }

    @Test
    fun `grammar_tags items are a oneOf per category with const category and enum forms`() {
        val schema =
            EnrichmentSchema
                .buildJsonSchema(
                    unitTypeIds = emptySet(),
                    complementIds = emptySet(),
                    usageAxesAndValues = emptyMap(),
                    grammarCategoriesAndForms =
                        mapOf(
                            "verb_irregular" to setOf("infinitive", "past_tense", "past_participle"),
                            "separability" to setOf("separable", "inseparable"),
                        ),
                ).jsonObject

        val tagItems = itemProperties(schema).obj("grammar_tags").obj("items")
        val byCategory = tagItems.arr("oneOf").associateBy { branchConst(it.jsonObject, key = "category") }

        assertEquals(setOf("separability", "verb_irregular"), byCategory.keys)
        assertEquals(
            listOf("infinitive", "past_participle", "past_tense"),
            branchEnum(byCategory.getValue("verb_irregular").jsonObject, valueKey = "form"),
        )
    }

    @Test
    fun `empty taxonomy inputs leave constrained fields as free strings or open objects`() {
        val props = itemProperties(buildEmpty())

        assertNull(props.obj("unit_type")["enum"])
        assertNull(props.obj("complementation").obj("items")["enum"])
        assertNull(props.obj("usage_labels").obj("items")["oneOf"])
        assertNull(props.obj("grammar_tags").obj("items")["oneOf"])
    }

    @Test
    fun `a colliding extension key never overwrites the built-in schema`() {
        val collider =
            object : AiEnrichmentExtension {
                override val id: String = "rogue"
                override val ownedKeys: Set<String> = setOf("translation")

                override fun fields(): List<EnrichmentSchema.Field> =
                    listOf(
                        EnrichmentSchema.Field(
                            serialName = "translation",
                            shape = EnrichmentSchema.ShapeType.ArrayOf(EnrichmentSchema.ShapeType.Text),
                            guidance = "list shape from a misconfigured extension",
                        ),
                    )
            }

        val schema =
            EnrichmentSchema
                .buildJsonSchema(
                    unitTypeIds = emptySet(),
                    complementIds = emptySet(),
                    usageAxesAndValues = emptyMap(),
                    grammarCategoriesAndForms = emptyMap(),
                    extensions = setOf(collider),
                ).jsonObject

        val translation = itemProperties(schema).obj("translation")
        assertEquals("string", translation.str("type"), "built-in wins, extension cannot redefine the field")
    }

    @Test
    fun `extensions splice their fields into the item schema alongside built-ins`() {
        val extension =
            object : AiEnrichmentExtension {
                override val id: String = "etymology"
                override val ownedKeys: Set<String> = setOf("etymology")

                override fun fields(): List<EnrichmentSchema.Field> =
                    listOf(
                        EnrichmentSchema.Field(
                            serialName = "etymology",
                            shape = EnrichmentSchema.ShapeType.Text,
                            guidance = "short origin note",
                        ),
                    )
            }

        val schema =
            EnrichmentSchema
                .buildJsonSchema(
                    unitTypeIds = emptySet(),
                    complementIds = emptySet(),
                    usageAxesAndValues = emptyMap(),
                    grammarCategoriesAndForms = emptyMap(),
                    extensions = setOf(extension),
                ).jsonObject

        val props = itemProperties(schema)
        assertTrue("etymology" in props.keys, "extension field is present in item properties")
        assertEquals("string", props.obj("etymology").str("type"))
    }

    private fun buildEmpty(): JsonObject =
        EnrichmentSchema.buildJsonSchema(emptySet(), emptySet(), emptyMap(), emptyMap()).jsonObject

    private fun itemProperties(schema: JsonObject): JsonObject =
        schema
            .obj("properties")
            .obj("items")
            .obj("items")
            .obj("properties")

    private fun branchConst(
        branch: JsonObject,
        key: String,
    ): String = branch.obj("properties").obj(key).str("const")

    private fun branchEnum(
        branch: JsonObject,
        valueKey: String,
    ): List<String> =
        branch
            .obj("properties")
            .obj(valueKey)
            .arr("enum")
            .asContentList()

    private fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

    private fun JsonObject.arr(key: String): JsonArray = getValue(key).jsonArray

    private fun JsonObject.str(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonArray.asContentList(): List<String> = map { it.jsonPrimitive.content }
}
