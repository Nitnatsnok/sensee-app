package app.sensee.feature.practice.presentation.impl.home

import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.feature.library.domain.Card
import app.sensee.feature.library.domain.CardId
import app.sensee.feature.library.domain.CatalogOrigin
import app.sensee.feature.library.domain.CatalogRepository
import app.sensee.feature.library.domain.Deck
import app.sensee.feature.library.domain.DeckId
import app.sensee.feature.library.domain.DeckWithCards
import app.sensee.feature.library.domain.Lemma
import app.sensee.feature.library.domain.LemmaId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PracticeHomeLogicTest {
    @Test
    fun `retry resubscribes after the deck stream fails`() {
        val catalog = FakeCatalog(failDeckStream = true)
        val logic = newLogic(catalog)

        assertIs<DataLoadingState.Error>(logic.uiState.value.loadingState)

        catalog.failDeckStream = false
        logic.retry()

        assertEquals(DataLoadingState.Success, logic.uiState.value.loadingState)
        assertEquals(
            listOf("owned"),
            logic.uiState.value.decks
                .map { it.id },
        )
    }

    private fun newLogic(catalog: FakeCatalog): PracticeHomeLogic =
        PracticeHomeLogic(
            catalogRepository = catalog,
            appDispatchers = immediateAppDispatchers(),
            appDiagnostics = noOpAppDiagnostics(),
        )

    private class FakeCatalog(
        var failDeckStream: Boolean = false,
    ) : CatalogRepository {
        private val decks = MutableStateFlow(listOf(deck("owned")))

        override fun observeOwnedMaterial(): Flow<List<Deck>> =
            if (failDeckStream) {
                flow { throw IllegalStateException("deck stream failed") }
            } else {
                decks.asStateFlow()
            }

        override suspend fun refreshFromRemote() = Unit

        override suspend fun loadDeck(deckId: DeckId): DeckWithCards = error("unused")

        override suspend fun loadCard(cardId: CardId): Card = error("unused")

        override suspend fun loadLemma(lemmaId: LemmaId): Lemma = error("unused")
    }

    private companion object {
        fun deck(id: String): Deck =
            Deck(
                id = DeckId(id),
                title = "Deck $id",
                description = "",
                cardCount = 0,
                origin = CatalogOrigin.Personal,
            )
    }
}
