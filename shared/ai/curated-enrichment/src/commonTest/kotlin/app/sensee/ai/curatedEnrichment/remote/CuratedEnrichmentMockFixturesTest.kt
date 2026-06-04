package app.sensee.ai.curatedEnrichment.remote

import app.sensee.ai.core.request.EnrichmentSchema
import app.sensee.ai.core.wire.EnrichmentResponseV1
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertTrue

class CuratedEnrichmentMockFixturesTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val fixtures = CuratedEnrichmentMockFixtures().fixtures

    @Test
    fun `every curated enrichment fixture is a non-empty wire response under a slug path`() {
        assertTrue(fixtures.isNotEmpty(), "expected at least one curated enrichment fixture")
        fixtures.forEach { (path, body) ->
            assertTrue(path.startsWith("enrichment/"), "fixture path must live under enrichment/: $path")
            val slug = path.removePrefix("enrichment/")
            assertTrue(
                slug == slug.lowercase() && ' ' !in slug,
                "lemma slug must be lowercased and space-free so the client can address it: '$slug'",
            )
            val response = json.decodeFromString(EnrichmentResponseV1.serializer(), body)
            assertTrue(response.items.isNotEmpty(), "fixture $path must carry at least one item")
            assertTrue(
                response.items.all { !it.translation.isNullOrBlank() },
                "every curated item in $path must carry a translation",
            )
        }
    }

    @Test
    fun `every curated item carries only known wire keys plus the curated cefr extension`() {
        // ignoreUnknownKeys hides stray/typo'd keys on decode; pin the key set
        // explicitly. cefr is allow-listed: CuratedAiEnrichmentClient surfaces it
        // through EnrichmentSuggestion.extensions, so it lives outside the wire DTO.
        val allowedKeys = EnrichmentSchema.builtInFieldNames + "cefr"
        fixtures.forEach { (path, body) ->
            val root = json.parseToJsonElement(body).jsonObject
            val items = root.getValue("items").jsonArray
            items.forEachIndexed { index, item ->
                val unknown = item.jsonObject.keys - allowedKeys
                assertTrue(
                    unknown.isEmpty(),
                    "fixture $path item $index has unknown key(s) $unknown; " +
                        "add the field to the wire DTO or remove the stray key",
                )
            }
        }
    }
}
