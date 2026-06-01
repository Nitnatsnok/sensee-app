package app.sensee.feature.library.data.remote

import app.sensee.ai.core.CefrEnrichmentExtension
import app.sensee.ai.core.EnrichmentResponseMapper
import app.sensee.ai.core.EnrichmentResponseV1
import app.sensee.ai.core.extractItemExtensions
import app.sensee.grammar.domain.ComponentRole
import app.sensee.grammar.domain.ComponentSalience
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.enrichment.toSense
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
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

    // Pins that a catalog card's `cefr` extension is NOT carried onto the persisted
    // Sense. The curated/LLM seam surfaces cefr through EnrichmentSuggestion.extensions,
    // but the catalog sync path (CatalogLocalDataSource.toConfirmedSenseOrNull) maps
    // WITHOUT itemExtensions on purpose: Sense/SenseDto have no cefr slot and nothing
    // reads it off a persisted Sense. Threading the extension through the mapper yields
    // the SAME Sense the catalog path produces, so the omission is lossless. If cefr
    // ever becomes part of Sense, the final assertion breaks and forces the catalog
    // path to thread it.
    @Test
    fun `a deck card cefr extension is not carried onto the persisted catalog sense`() {
        // A flat catalog card (CardDtoSerializer shape) that authors a `cefr` value.
        val cardJson =
            """
            {
              "id": "card-cefr-probe",
              "lemma_id": "lemma-resilient",
              "translation": "устойчивый",
              "surface_form": "resilient",
              "unit_type": "adjective",
              "cefr": "B2"
            }
            """.trimIndent()

        val card = json.decodeFromString(CardDto.serializer(), cardJson)

        // The catalog path maps the typed item with NO extensions.
        val catalogSense =
            EnrichmentResponseMapper
                .map(EnrichmentResponseV1(items = listOf(card.enrichment)))
                .suggestions
                .single()
                .toSense(fallbackTerm = "resilient")

        // The hypothetical "threaded" path extracts cefr from the raw card JSON
        // (minus catalog identity, mirroring CardDtoSerializer) and feeds it in.
        val rawEnrichment =
            JsonObject(
                json
                    .parseToJsonElement(cardJson)
                    .jsonObject
                    .filterKeys { it != "id" && it != "lemma_id" },
            )
        val extensions = setOf(CefrEnrichmentExtension).extractItemExtensions(rawEnrichment)
        val threadedSuggestion =
            EnrichmentResponseMapper
                .map(EnrichmentResponseV1(items = listOf(card.enrichment)), listOf(extensions))
                .suggestions
                .single()

        // The extension channel really did capture cefr...
        assertEquals(JsonPrimitive("B2"), threadedSuggestion.extensions["cefr"])
        // ...yet the persisted Sense is identical whether or not it is threaded.
        assertEquals(catalogSense, threadedSuggestion.toSense(fallbackTerm = "resilient"))
    }
}
