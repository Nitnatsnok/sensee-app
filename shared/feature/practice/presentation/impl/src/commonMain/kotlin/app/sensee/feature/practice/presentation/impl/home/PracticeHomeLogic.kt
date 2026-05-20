package app.sensee.feature.practice.presentation.impl.home

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.decompose.logic.BaseLogic
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.library.domain.CatalogRepository
import app.sensee.feature.library.domain.Deck
import app.sensee.feature.practice.presentation.api.DeckSummaryUiState
import app.sensee.feature.practice.presentation.api.PracticeHomeUiState
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AssistedInject
public class PracticeHomeLogic(
    private val catalogRepository: CatalogRepository,
    appDispatchers: AppDispatchers,
    appDiagnostics: AppDiagnostics,
) : BaseLogic(appDispatchers, appDiagnostics) {
    @AssistedFactory
    public fun interface Factory {
        public fun create(): PracticeHomeLogic
    }

    private val mutableUiState = MutableStateFlow(PracticeHomeUiState())
    private var observationJob: Job? = null

    public val uiState: StateFlow<PracticeHomeUiState> = mutableUiState.asStateFlow()

    init {
        observe()
        refreshFromRemote()
    }

    private fun observe() {
        observationJob?.cancel()
        observationJob =
            catalogRepository
                .observeOwnedMaterial()
                .onEach { decks ->
                    mutableUiState.update {
                        PracticeHomeUiState(
                            loadingState = DataLoadingState.Success,
                            decks = decks.map(Deck::toUi).toPersistentList(),
                        )
                    }
                }.catch { throwable ->
                    logger.error(throwable) { "Practice deck stream failed" }
                    crashReporter.recordException(
                        throwable,
                        attributes =
                            mapOf(
                                "area" to "practice",
                                "operation" to "observe_decks",
                            ),
                    )
                    mutableUiState.update { it.copy(loadingState = DataLoadingState.Error(throwable)) }
                }.launchIn(logicScope)
    }

    public fun retry() {
        observe()
        refreshFromRemote()
    }

    private fun refreshFromRemote() {
        logicScope.launch {
            runCatchingCancellable { catalogRepository.refreshFromRemote() }
                .onFailure { throwable ->
                    logger.error(throwable) { "Practice remote refresh failed" }
                    // The stream keeps serving the local cache, so only surface the error
                    // when there is nothing else to show.
                    mutableUiState.update { state ->
                        if (state.decks.isEmpty()) {
                            state.copy(loadingState = DataLoadingState.Error(throwable))
                        } else {
                            state
                        }
                    }
                }
        }
    }
}

private fun Deck.toUi(): DeckSummaryUiState =
    DeckSummaryUiState(
        id = id.value,
        title = title,
        description = description,
        cardCount = cardCount,
    )
