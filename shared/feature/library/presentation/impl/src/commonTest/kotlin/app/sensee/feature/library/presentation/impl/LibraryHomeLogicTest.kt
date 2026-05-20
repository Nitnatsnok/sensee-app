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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LibraryHomeLogicTest {
    @Test
    fun `library home separates service suggestions from owned material`() {
        val logic = newLogic(FakeCatalog())

        val state = logic.uiState.value
        assertEquals(listOf("svc"), state.suggested.map { it.id })
        assertEquals(setOf("adopted", "captured"), state.owned.map { it.id }.toSet())
    }

    @Test
    fun `the captured deck is shown as owned and is not un-adoptable`() {
        val logic = newLogic(FakeCatalog())

        val owned = logic.uiState.value.owned
        val captured = owned.single { it.id == "captured" }
        val adopted = owned.single { it.id == "adopted" }
        assertFalse(captured.canUnAdopt, "the derived captured deck cannot be detached")
        assertTrue(adopted.canUnAdopt, "an adopted deck can be detached")
    }

    @Test
    fun `adopt action moves a service set into owned material without flicker`() {
        val catalog = FakeCatalog()
        val logic = newLogic(catalog)

        logic.adopt("svc")

        val state = logic.uiState.value
        assertTrue(state.suggested.isEmpty(), "an adopted set leaves the suggestions list")
        assertTrue(state.owned.any { it.id == "svc" }, "an adopted set joins owned material")
        assertTrue("svc" in catalog.adopted.value, "adopt was delegated to the adoption repository")
    }

    @Test
    fun `retry resubscribes after the library stream fails`() {
        val catalog = FakeCatalog(failOwnedStream = true)
        val logic = newLogic(catalog)

        assertIs<DataLoadingState.Error>(logic.uiState.value.loadingState)

        catalog.failOwnedStream = false
        logic.retry()

        assertEquals(DataLoadingState.Success, logic.uiState.value.loadingState)
        assertEquals(
            setOf("adopted", "captured"),
            logic.uiState.value.owned
                .map { it.id }
                .toSet(),
        )
    }

    private fun newLogic(catalog: FakeCatalog): LibraryHomeLogic =
        LibraryHomeLogic(
            catalogRepository = catalog,
            adoptionRepository = catalog,
            appDispatchers = immediateAppDispatchers(),
            appDiagnostics = noOpAppDiagnostics(),
        )

    // Reactive fake: backing StateFlows let `adopt` mutate them and have the
    // logic's combine() pick the change up just like a real SQLDelight stream.
    private class FakeCatalog(
        var failOwnedStream: Boolean = false,
    ) : CatalogRepository,
        CatalogAdoptionRepository {
        val adopted = MutableStateFlow(setOf("adopted"))
        private val serviceDecks = MutableStateFlow(setOf("svc"))

        override fun observeOwnedMaterial(): Flow<List<Deck>> =
            if (failOwnedStream) {
                flow { throw IllegalStateException("owned stream failed") }
            } else {
                adopted.map { ids -> ids.map { deck(it, CatalogOrigin.Personal) } + capturedDeck }
            }

        override suspend fun refreshFromRemote() = Unit

        override suspend fun loadDeck(deckId: DeckId): DeckWithCards =
            DeckWithCards(deck(deckId.value, CatalogOrigin.Personal), emptyList())

        override suspend fun loadCard(cardId: CardId): Card = error("unused")

        override suspend fun loadLemma(lemmaId: LemmaId): Lemma = error("unused")

        override suspend fun adopt(deckId: DeckId) {
            serviceDecks.update { it - deckId.value }
            adopted.update { it + deckId.value }
        }

        override suspend fun unAdopt(deckId: DeckId) {
            adopted.update { it - deckId.value }
        }

        override fun observeAdoptedDecks(): Flow<List<Deck>> =
            adopted.asStateFlow().map { ids -> ids.map { deck(it, CatalogOrigin.Personal) } }

        override fun observeSuggestedDecks(): Flow<List<Deck>> =
            serviceDecks.asStateFlow().map { ids -> ids.map { deck(it, CatalogOrigin.Service) } }
    }

    private companion object {
        val capturedDeck: Deck = deck("captured", CatalogOrigin.Personal)

        fun deck(
            id: String,
            origin: CatalogOrigin,
        ): Deck =
            Deck(
                id = DeckId(id),
                title = "Deck $id",
                description = "",
                cardCount = 0,
                origin = origin,
            )
    }
}
