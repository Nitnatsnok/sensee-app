package app.sensee.feature.library.data.remote

import app.sensee.grammar.domain.ComponentRole
import app.sensee.grammar.domain.ComponentSalience
import app.sensee.lexicon.domain.CefrLevel
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.serialization.toDomain
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Pins that a service deck card carries ideal sense content: its SenseDto maps
// into the same rich Sense the capture path persists, through the shared
// `SenseDto.toDomain()` mapper that CatalogLocalDataSource uses on ingest.
class ServiceDeckSenseFixtureTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val fixtures = CatalogMockFixtures().fixtures

    private fun senseOf(
        deckId: String,
        cardId: String,
    ): Sense =
        json
            .decodeFromString<DeckDto>(fixtures.getValue("practice/decks/$deckId"))
            .cards
            .single { it.id == cardId }
            .sense
            .toDomain()

    @Test
    fun `a service deck card maps into a rich sense with component salience`() {
        val sense = senseOf("phrasal-verbs-come", "card-come-up")

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
        val sense = senseOf("phrasal-verbs-come", "card-come")

        assertTrue(sense.wordFamily.any { it.lemma == "comeback" }, "word family reaches the sense")
        assertEquals("came", sense.irregularForms?.past)
    }

    @Test
    fun `a shared card maps to one identical sense across the decks that reference it`() {
        val fromPhrasal = senseOf("phrasal-verbs-come", "card-come-across")
        val fromDaily = senseOf("daily-essentials", "card-come-across")

        assertEquals(fromPhrasal, fromDaily)
    }

    // cefr is a first-class Sense attribute. The service wire is the canonical
    // SenseDto with a direct cefr slot, so a catalog sense threads cefr the same
    // way a captured sense does — no extension channel needed.
    @Test
    fun `cefr threads onto a service sense through the SenseDto wire`() {
        assertEquals(CefrLevel.B2, senseOf("architecture-basics", "card-resilient").cefr)
    }
}
