package app.sensee.feature.library.data.remote

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogMockFixturesTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val fixtures = CatalogMockFixtures().fixtures

    @Test
    fun `every service deck advertises ten cards`() {
        val list = json.decodeFromString<DeckListDto>(fixtures.getValue("practice/decks"))
        assertEquals(3, list.decks.size)
        list.decks.forEach { deck ->
            assertEquals(10, deck.cardCount, "${deck.id} should advertise 10 cards")
            val detail = json.decodeFromString<DeckDto>(fixtures.getValue("practice/decks/${deck.id}"))
            assertEquals(10, detail.cards.size, "${deck.id} should contain 10 cards")
        }
    }

    @Test
    fun `shared mock cards appear in every deck that references them with one identity`() {
        val byDeck =
            listOf("phrasal-verbs-come", "architecture-basics", "daily-essentials")
                .associateWith { id ->
                    json.decodeFromString<DeckDto>(fixtures.getValue("practice/decks/$id")).cards
                }

        val expectedMembership =
            mapOf(
                "card-come-up" to setOf("phrasal-verbs-come", "daily-essentials"),
                "card-come-across" to setOf("phrasal-verbs-come", "daily-essentials"),
                "card-resilient" to setOf("architecture-basics", "daily-essentials"),
                "card-eventually" to setOf("architecture-basics", "daily-essentials"),
            )

        expectedMembership.forEach { (cardId, decks) ->
            val copies = byDeck.filterValues { cards -> cards.any { it.id == cardId } }
            assertEquals(decks, copies.keys, "$cardId must be shared by exactly these decks")
            val distinct = decks.map { deck -> byDeck.getValue(deck).single { it.id == cardId } }.distinct()
            assertEquals(
                1,
                distinct.size,
                "$cardId must be byte-identical across decks so it is a single card row",
            )
        }
    }

    @Test
    fun `each card has a lemma fixture endpoint`() {
        val deckIds = listOf("phrasal-verbs-come", "architecture-basics", "daily-essentials")
        val lemmaIds =
            deckIds
                .flatMap { id -> json.decodeFromString<DeckDto>(fixtures.getValue("practice/decks/$id")).cards }
                .map { it.lemmaId }
                .toSet()
        lemmaIds.forEach { lemmaId ->
            assertTrue(
                fixtures.containsKey("practice/lemmas/$lemmaId"),
                "missing lemma fixture for $lemmaId",
            )
        }
    }
}
