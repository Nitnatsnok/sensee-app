package app.sensee.feature.library.presentation.impl

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.decompose.logic.BaseLogic
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.library.domain.CatalogAdoptionRepository
import app.sensee.feature.library.domain.CatalogRepository
import app.sensee.feature.library.domain.Deck
import app.sensee.feature.library.domain.DeckId
import app.sensee.feature.library.presentation.api.LibraryDeckUiState
import app.sensee.feature.library.presentation.api.LibraryHomeUiState
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [catalogRepository] streams the user's owned material (adopted decks + the
 * derived captured deck). [adoptionRepository] streams the Service suggestions
 * to adopt and the set of decks adopted (hence detachable) — the captured deck
 * is owned but not detachable.
 *
 * Reactive: once subscribed, adopt/un-adopt are pure repository mutations; the
 * UI updates from the underlying SQLDelight stream without a full reload. Remote
 * sync of the Service catalog is an explicit one-shot triggered on first start
 * and on Retry.
 */
@AssistedInject
public class LibraryHomeLogic(
    private val catalogRepository: CatalogRepository,
    private val adoptionRepository: CatalogAdoptionRepository,
    appDispatchers: AppDispatchers,
    appDiagnostics: AppDiagnostics,
) : BaseLogic(appDispatchers, appDiagnostics) {
    @AssistedFactory
    public fun interface Factory {
        public fun create(): LibraryHomeLogic
    }

    private val mutableUiState = MutableStateFlow(LibraryHomeUiState())
    private var observationJob: Job? = null

    public val uiState: StateFlow<LibraryHomeUiState> = mutableUiState.asStateFlow()

    init {
        observe()
        refreshFromRemote()
    }

    private fun observe() {
        observationJob?.cancel()
        observationJob =
            combine(
                catalogRepository.observeOwnedMaterial(),
                adoptionRepository.observeAdoptedDecks(),
                adoptionRepository.observeSuggestedDecks(),
            ) { owned, adopted, suggested ->
                val detachableIds = adopted.mapTo(mutableSetOf()) { it.id.value }
                LibraryHomeUiState(
                    // The stream is the source of truth: once it emits, the screen has data.
                    // A reload from Retry does not flip this back to Loading — see refreshFromRemote.
                    loadingState = DataLoadingState.Success,
                    suggested = suggested.map { it.toUi(canUnAdopt = false) }.toPersistentList(),
                    owned = owned.map { it.toUi(canUnAdopt = it.id.value in detachableIds) }.toPersistentList(),
                )
            }.onEach { next -> mutableUiState.update { next } }
                .catch { throwable ->
                    logger.error(throwable) { "Library stream failed" }
                    crashReporter.recordException(
                        throwable,
                        attributes =
                            mapOf(
                                "area" to "library",
                                "operation" to "observe_home",
                            ),
                    )
                    mutableUiState.update { it.copy(loadingState = DataLoadingState.Error(throwable)) }
                }.launchIn(logicScope)
    }

    public fun retry() {
        // Retry covers both failure modes: recreate the local stream if it
        // terminated, then ask the Service catalog to refresh its cache.
        observe()
        refreshFromRemote()
    }

    public fun adopt(deckId: String) {
        mutate { adoptionRepository.adopt(DeckId(deckId)) }
    }

    public fun unAdopt(deckId: String) {
        mutate { adoptionRepository.unAdopt(DeckId(deckId)) }
    }

    private fun refreshFromRemote() {
        logicScope.launch {
            runCatchingCancellable { catalogRepository.refreshFromRemote() }
                .onFailure { throwable ->
                    logger.error(throwable) { "Library remote refresh failed" }
                    // The stream keeps serving the local cache, so surface the error only
                    // when there is nothing to fall back on (initial empty state).
                    mutableUiState.update { state ->
                        if (state.owned.isEmpty() && state.suggested.isEmpty()) {
                            state.copy(loadingState = DataLoadingState.Error(throwable))
                        } else {
                            state
                        }
                    }
                }
        }
    }

    private fun mutate(block: suspend () -> Unit) {
        logicScope.launch {
            runCatchingCancellable { block() }
                .onFailure { throwable ->
                    logger.error(throwable) { "Library adoption change failed" }
                    mutableUiState.update { it.copy(loadingState = DataLoadingState.Error(throwable)) }
                }
        }
    }
}

private fun Deck.toUi(canUnAdopt: Boolean): LibraryDeckUiState =
    LibraryDeckUiState(
        id = id.value,
        title = title,
        description = description,
        cardCount = cardCount,
        canUnAdopt = canUnAdopt,
    )
