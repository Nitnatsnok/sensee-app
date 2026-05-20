package app.sensee.feature.library.data.remote

import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

// The grammar/usage/complement taxonomy itself is owned and drift-tested by
// shared/grammar/data. This only pins that the catalog's own deck fixtures
// carry grammar tags whose ids stay within the domain invariant.
class DeckGrammarTagsFixtureTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deck fixtures expose known grammar tags`() {
        val categoryIds = GrammarCategory.entries.map { it.id }.toSet()
        val formIds = GrammarForm.entries.map { it.id }.toSet()
        val deckFixtures =
            CatalogMockFixtures()
                .fixtures
                .filterKeys { it.startsWith("practice/decks/") }

        deckFixtures.forEach { (path, fixture) ->
            val deck = json.decodeFromString<DeckDto>(fixture)
            deck.cards.forEach { card ->
                assertEquals(
                    true,
                    card.grammarTags.isNotEmpty(),
                    "Missing grammar tags for $path/${card.id}",
                )
                card.grammarTags.forEach { tag ->
                    assertEquals(
                        true,
                        tag.category in categoryIds,
                        "Unknown grammar tag category for $path/${card.id}: ${tag.category}",
                    )
                    assertEquals(
                        true,
                        tag.form in formIds,
                        "Unknown grammar tag form for $path/${card.id}: ${tag.form}",
                    )
                }
            }
        }
    }
}
