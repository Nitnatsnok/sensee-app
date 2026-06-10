package app.sensee.feature.library.presentation.impl

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.decompose.logic.BaseLogic
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.library.domain.Card
import app.sensee.feature.library.domain.CatalogAdoptionRepository
import app.sensee.feature.library.domain.CatalogOrigin
import app.sensee.feature.library.domain.CatalogRepository
import app.sensee.feature.library.domain.DeckId
import app.sensee.feature.library.presentation.api.LibraryCardUiState
import app.sensee.feature.library.presentation.api.LibraryDeckDetailUiState
import app.sensee.grammar.domain.GrammarLabelsLoadResult
import app.sensee.grammar.domain.GrammarLabelsProvider
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Read-only browse of a deck's cards. [catalogRepository] projects the cards without writing
 * ([CatalogRepository.previewDeck]): an owned/captured deck is read locally, a Service
 * suggestion is fetched from remote and never ingested. Adopting the deck is a separate,
 * explicit action ([adopt]) routed through [adoptionRepository] — the only path that writes.
 */
@AssistedInject
public class LibraryDeckDetailLogic(
    @Assisted private val deckId: String,
    private val catalogRepository: CatalogRepository,
    private val adoptionRepository: CatalogAdoptionRepository,
    private val grammarLabelsProvider: GrammarLabelsProvider,
    appDispatchers: AppDispatchers,
    appDiagnostics: AppDiagnostics,
) : BaseLogic(appDispatchers, appDiagnostics) {
    @AssistedFactory
    public fun interface Factory {
        public fun create(deckId: String): LibraryDeckDetailLogic
    }

    public val uiState: StateFlow<LibraryDeckDetailUiState>
        field =
        MutableStateFlow(LibraryDeckDetailUiState(grammarLabels = grammarLabelsProvider.cachedLabels()))

    init {
        load()
        loadGrammarLabels()
    }

    // Best-effort: the cached snapshot seeds the state and an async refresh upgrades the card
    // badges in place. A failed load keeps EMPTY labels, so the cards degrade to raw ids.
    private fun loadGrammarLabels() {
        logicScope.launch {
            val result = grammarLabelsProvider.awaitLabels()
            if (result is GrammarLabelsLoadResult.Loaded) {
                uiState.update { it.copy(grammarLabels = result.labels) }
            }
        }
    }

    public fun load() {
        logicScope.launch {
            uiState.update { it.copy(loadingState = DataLoadingState.Loading, adoptError = null) }
            runCatchingCancellable { catalogRepository.previewDeck(DeckId(deckId)) }
                .onSuccess { deck ->
                    // copy (not a fresh state) so an already-resolved grammarLabels survives.
                    uiState.update {
                        it.copy(
                            loadingState = DataLoadingState.Success,
                            title = deck.deck.title,
                            description = deck.deck.description,
                            cards = deck.cards.mapNotNull(Card::toUiOrNull).toPersistentList(),
                            isService = deck.deck.origin == CatalogOrigin.Service,
                            adoptError = null,
                        )
                    }
                }.onFailure { throwable ->
                    logger.error(throwable) { "Failed to preview deck $deckId" }
                    crashReporter.recordException(
                        throwable,
                        attributes =
                            mapOf(
                                "area" to "library",
                                "operation" to "preview_deck",
                            ),
                    )
                    uiState.update { it.copy(loadingState = DataLoadingState.Error(throwable)) }
                }
        }
    }

    public fun adopt() {
        logicScope.launch {
            uiState.update { it.copy(adopting = true, adoptError = null) }
            runCatchingCancellable { adoptionRepository.adopt(DeckId(deckId)) }
                .onSuccess {
                    // The deck is now owned material; drop the Service affordance in place.
                    uiState.update { state ->
                        state.copy(isService = false, adopting = false, adoptError = null)
                    }
                }.onFailure { throwable ->
                    logger.error(throwable) { "Failed to adopt deck $deckId" }
                    uiState.update { it.copy(adopting = false, adoptError = throwable) }
                }
        }
    }
}

// Catalog/preview projection always carries the rich sense (SenseCatalogProjection.cardFromSense);
// a card without one cannot render the shared sense card, so it is defensively skipped.
private fun Card.toUiOrNull(): LibraryCardUiState? = sense?.let { LibraryCardUiState(id = id.value, sense = it) }
