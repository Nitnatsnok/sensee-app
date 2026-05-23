package app.sensee.settings.data.topic

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TopicCatalogMockFixturesTest {
    @Test
    fun `mock backend serves a well-formed topic catalog`() {
        val raw = TopicCatalogMockFixtures().fixtures.getValue("learning/topics")

        val catalog = Json.decodeFromString<TopicCatalogDto>(raw)
        val ids = catalog.topics.map { it.id }

        assertTrue(catalog.topics.isNotEmpty(), "the catalog is not empty")
        assertEquals(ids.size, ids.toSet().size, "topic ids are unique")
        assertTrue(
            catalog.topics.all {
                it.id.isNotBlank() && it.displayName.isNotBlank() && it.promptKeyword.isNotBlank()
            },
            "every topic carries an id, a display name and a prompt keyword",
        )
    }
}
