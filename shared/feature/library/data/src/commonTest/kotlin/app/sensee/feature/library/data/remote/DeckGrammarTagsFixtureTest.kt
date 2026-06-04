package app.sensee.feature.library.data.remote

import app.sensee.grammar.domain.GrammarTag
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
        val allowedFormsByCategory = GrammarTag.knownAllowedFormsByCategory
        val deckFixtures =
            CatalogMockFixtures()
                .fixtures
                .filterKeys { it.startsWith("practice/decks/") }

        deckFixtures.forEach { (path, fixture) ->
            val deck = json.decodeFromString<DeckDto>(fixture)
            deck.cards.forEach { card ->
                // Grammar tags live on the card's canonical SenseDto.
                val grammarTags = card.sense.grammarTags
                assertEquals(
                    true,
                    grammarTags.isNotEmpty(),
                    "Missing grammar tags for $path/${card.id}",
                )
                grammarTags.forEach { tag ->
                    val allowedForms = allowedFormsByCategory[tag.category].orEmpty()
                    assertEquals(
                        true,
                        allowedForms.isNotEmpty(),
                        "Unknown grammar tag category for $path/${card.id}: ${tag.category}",
                    )
                    assertEquals(
                        true,
                        tag.form in allowedForms,
                        "Invalid grammar tag pair for $path/${card.id}: ${tag.category}/${tag.form}",
                    )
                }
            }
        }
    }
}
