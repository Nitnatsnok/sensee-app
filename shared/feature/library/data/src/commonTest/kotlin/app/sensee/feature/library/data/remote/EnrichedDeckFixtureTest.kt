package app.sensee.feature.library.data.remote

import app.sensee.ai.core.EnrichmentResponseMapper
import app.sensee.ai.core.EnrichmentResponseV1
import app.sensee.grammar.domain.ComponentRole
import app.sensee.grammar.domain.ComponentSalience
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.enrichment.toSense
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Pins that the service catalog carries ideal-enrichment content: a deck card's
// enrichment item rebuilds into the same rich Sense the capture path produces,
// through the same public boundary mappers CatalogLocalDataSource uses.
class EnrichedDeckFixtureTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val fixtures = CatalogMockFixtures().fixtures

    private fun senseOf(
        deckId: String,
        cardId: String,
        fallbackTerm: String,
    ): Sense {
        val card =
            json
                .decodeFromString<DeckDto>(fixtures.getValue("practice/decks/$deckId"))
                .cards
                .single { it.id == cardId }
        return EnrichmentResponseMapper
            .map(EnrichmentResponseV1(items = listOf(card.enrichment)))
            .suggestions
            .single()
            .toSense(fallbackTerm = fallbackTerm)
    }

    @Test
    fun `a service deck card rebuilds into a rich sense with component salience`() {
        val sense = senseOf("phrasal-verbs-come", "card-come-up", "come up")

        assertEquals("возникать, неожиданно появиться", sense.translation)
        assertEquals(
            listOf("come" to ComponentSalience.Primary, "up" to ComponentSalience.Secondary),
            sense.components.map { it.text to it.salience },
        )
        assertEquals(ComponentRole.Head, sense.components.first().role)
        assertTrue(sense.synonyms.isNotEmpty(), "synonyms reach the sense")
        val example = sense.contextualApplications.single()
        assertTrue(example.alignment.isNotEmpty(), "example alignment reaches the sense")
    }

    @Test
    fun `the base verb card carries its word family and principal parts`() {
        val sense = senseOf("phrasal-verbs-come", "card-come", "come")

        assertTrue(sense.wordFamily.any { it.lemma == "comeback" }, "word family reaches the sense")
        assertEquals("came", sense.irregularForms?.past)
    }

    @Test
    fun `a shared enriched card is identical across the decks that reference it`() {
        val fromPhrasal = senseOf("phrasal-verbs-come", "card-come-across", "come across")
        val fromDaily = senseOf("daily-essentials", "card-come-across", "come across")

        assertEquals(fromPhrasal, fromDaily)
    }
}
