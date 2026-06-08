package app.sensee.feature.library.data

import app.sensee.feature.library.data.remote.CardDto
import app.sensee.feature.library.data.remote.CatalogMockFixtures
import app.sensee.feature.library.data.remote.DeckDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The read-only Service-deck preview projects a remote [DeckDto] into cards without
 * touching any store (no store is even reachable from the pure mapper). Its renderable
 * card list mirrors the confirm-gate of an adopt, while deck meta keeps the original
 * source count shown for a Service/subscribed set.
 */
class ServiceDeckPreviewTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `previewing a service deck drops a non-confirmable card just like an adopt would`() {
        val good =
            json.decodeFromString<DeckDto>(
                CatalogMockFixtures().fixtures.getValue("practice/decks/phrasal-verbs-come"),
            )
        // A sense with no example fails the confirm-gate (needs a translation and an example).
        val bad =
            json.decodeFromString(
                CardDto.serializer(),
                """{"id":"card-bad","lemma_id":"lemma-bad","sense":{"translation":"плохой","surfaceForm":"bad","unitType":"adjective"}}""",
            )

        val preview = good.copy(cards = good.cards + bad).toPreviewDeckWithCards()

        assertTrue(preview.cards.isNotEmpty(), "confirmable cards are previewed")
        assertFalse(
            preview.cards.any { it.id.value == "card-bad" },
            "a card with no example is dropped, not previewed as a broken card",
        )
        assertEquals(good.cards.size + 1, preview.deck.cardCount, "the deck count reflects the source set")
        assertTrue(preview.cards.all { it.sense != null }, "each previewed card carries its rich sense")
    }
}
