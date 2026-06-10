package app.sensee.feature.library.presentation.impl

import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.feature.library.domain.Card
import app.sensee.feature.library.domain.CardId
import app.sensee.feature.library.domain.CatalogAdoptionRepository
import app.sensee.feature.library.domain.CatalogOrigin
import app.sensee.feature.library.domain.CatalogRepository
import app.sensee.feature.library.domain.Deck
import app.sensee.feature.library.domain.DeckId
import app.sensee.feature.library.domain.DeckWithCards
import app.sensee.feature.library.domain.Lemma
import app.sensee.feature.library.domain.LemmaId
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarLabelsProvider
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.lexicon.domain.Sense
import kotlinx.coroutines.flow.Flow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LibraryDeckDetailLogicTest {
    @Test
    fun `opening a deck shows its cards`() {
        val logic =
            newLogic(FakeCatalog(deck = deckOf("d1", CatalogOrigin.Personal, card("c1", "come across", "наткнуться"))))

        val state = logic.uiState.value
        assertEquals(DataLoadingState.Success, state.loadingState)
        assertEquals("Deck d1", state.title)
        assertEquals(listOf("c1"), state.cards.map { it.id })
        val card = state.cards.single()
        assertEquals("наткнуться", card.sense.translation)
        assertFalse(state.isService, "a personal deck offers no adopt action")
    }

    @Test
    fun `a service deck is browsable and offers adoption`() {
        val logic = newLogic(FakeCatalog(deck = deckOf("svc", CatalogOrigin.Service, card("c1", "deadline", "срок"))))

        assertTrue(logic.uiState.value.isService, "an unadopted service deck offers an explicit add action")
    }

    @Test
    fun `a failed preview surfaces an error`() {
        val logic = newLogic(FakeCatalog(failPreview = true))

        assertIs<DataLoadingState.Error>(logic.uiState.value.loadingState)
    }

    @Test
    fun `retry reloads after a failed preview`() {
        val catalog = FakeCatalog(failPreview = true, deck = deckOf("d1", CatalogOrigin.Personal, card("c1", "x", "y")))
        val logic = newLogic(catalog)
        assertIs<DataLoadingState.Error>(logic.uiState.value.loadingState)

        catalog.failPreview = false
        logic.load()

        assertEquals(DataLoadingState.Success, logic.uiState.value.loadingState)
    }

    @Test
    fun `adopting a service deck delegates to the repository and drops the add action`() {
        val catalog = FakeCatalog(deck = deckOf("svc", CatalogOrigin.Service, card("c1", "x", "y")))
        val logic = newLogic(catalog, deckId = "svc")
        assertTrue(logic.uiState.value.isService)

        logic.adopt()

        assertTrue("svc" in catalog.adopted, "adopt is delegated to the adoption repository")
        assertFalse(logic.uiState.value.isService, "after adoption the deck is owned, so the add action goes away")
        assertEquals(null, logic.uiState.value.adoptError)
    }

    @Test
    fun `failed adoption keeps the loaded service deck visible`() {
        val catalog =
            FakeCatalog(
                deck = deckOf("svc", CatalogOrigin.Service, card("c1", "deadline", "срок")),
                failAdopt = true,
            )
        val logic = newLogic(catalog, deckId = "svc")

        logic.adopt()

        val state = logic.uiState.value
        assertEquals(DataLoadingState.Success, state.loadingState)
        assertTrue(state.isService, "the add action stays available for a retry")
        assertFalse(state.adopting)
        assertEquals(listOf("c1"), state.cards.map { it.id }, "the previewed cards stay visible")
        assertIs<IllegalStateException>(state.adoptError)
    }

    private fun newLogic(
        catalog: FakeCatalog,
        deckId: String = "d1",
    ): LibraryDeckDetailLogic =
        LibraryDeckDetailLogic(
            deckId = deckId,
            catalogRepository = catalog,
            adoptionRepository = catalog,
            grammarLabelsProvider = GrammarLabelsProvider { GrammarLabels.EMPTY },
            appDispatchers = immediateAppDispatchers(),
            appDiagnostics = noOpAppDiagnostics(),
        )

    private fun deckOf(
        id: String,
        origin: CatalogOrigin,
        vararg cards: Card,
    ): DeckWithCards =
        DeckWithCards(
            deck =
                Deck(
                    id = DeckId(id),
                    title = "Deck $id",
                    description = "desc",
                    cardCount = cards.size,
                    origin = origin,
                ),
            cards = cards.toList(),
        )

    private fun card(
        id: String,
        headword: String,
        translation: String,
    ): Card =
        Card(
            id = CardId(id),
            lemmaId = LemmaId("lemma-$id"),
            headword = headword,
            translation = translation,
            contextSentence = "",
            unitType = GrammarUnitType.Phrase,
            grammarTags = emptyList(),
            senseSummary = translation,
            explanation = "",
            // The deck-detail card renders from the rich sense; a card without one is skipped.
            sense = Sense(translation = translation, surfaceForm = SurfaceForm.parse(headword), explanation = ""),
        )

    private class FakeCatalog(
        private val deck: DeckWithCards =
            DeckWithCards(
                Deck(DeckId("d1"), "Deck d1", "", 0, CatalogOrigin.Personal),
                emptyList(),
            ),
        var failPreview: Boolean = false,
        var failAdopt: Boolean = false,
    ) : CatalogRepository,
        CatalogAdoptionRepository {
        val adopted: MutableSet<String> = mutableSetOf()

        override suspend fun previewDeck(deckId: DeckId): DeckWithCards =
            if (failPreview) error("preview failed") else deck

        override suspend fun adopt(deckId: DeckId) {
            if (failAdopt) error("adopt failed")
            adopted += deckId.value
        }

        override suspend fun unAdopt(deckId: DeckId): Unit = error("unused")

        override fun observeAdoptedDecks(): Flow<List<Deck>> = error("unused")

        override fun observeSuggestedDecks(): Flow<List<Deck>> = error("unused")

        override fun observeOwnedMaterial(): Flow<List<Deck>> = error("unused")

        override suspend fun refreshFromRemote(): Unit = error("unused")

        override suspend fun loadDeck(deckId: DeckId): DeckWithCards = error("unused")

        override suspend fun loadCard(cardId: CardId): Card = error("unused")

        override suspend fun loadCards(cardIds: List<CardId>): List<Card> = error("unused")

        override suspend fun loadLemma(lemmaId: LemmaId): Lemma = error("unused")
    }
}
