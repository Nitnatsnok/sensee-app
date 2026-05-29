package app.sensee.feature.practice.presentation.impl.detail

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.decompose.logic.BaseLogic
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.library.domain.Card
import app.sensee.feature.library.domain.CardId
import app.sensee.feature.library.domain.CardSummary
import app.sensee.feature.library.domain.CatalogRepository
import app.sensee.feature.practice.presentation.api.CardDetailCardUiState
import app.sensee.feature.practice.presentation.api.CardDetailUiState
import app.sensee.feature.practice.presentation.api.RelatedCardUiState
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarLabelsLoadResult
import app.sensee.grammar.domain.GrammarLabelsProvider
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AssistedInject
public class CardDetailLogic(
    @Assisted private val cardId: String,
    private val catalogRepository: CatalogRepository,
    private val grammarLabelsProvider: GrammarLabelsProvider,
    appDispatchers: AppDispatchers,
    appDiagnostics: AppDiagnostics,
) : BaseLogic(appDispatchers, appDiagnostics) {
    @AssistedFactory
    public fun interface Factory {
        public fun create(cardId: String): CardDetailLogic
    }

    private val mutableUiState =
        grammarLabelsProvider.cachedLabels().let { initial ->
            MutableStateFlow(
                CardDetailUiState(
                    grammarLabels = initial,
                    grammarLabelsState = initialLabelsState(initial),
                ),
            )
        }

    public val uiState: StateFlow<CardDetailUiState> = mutableUiState.asStateFlow()

    init {
        load()
        loadGrammarLabels()
    }

    internal fun retryGrammarLabels() = loadGrammarLabels()

    private fun loadGrammarLabels() {
        logicScope.launch {
            mutableUiState.update { it.copy(grammarLabelsState = DataLoadingState.Loading) }
            when (val result = grammarLabelsProvider.awaitLabels()) {
                is GrammarLabelsLoadResult.Loaded ->
                    mutableUiState.update {
                        it.copy(
                            grammarLabels = result.labels,
                            grammarLabelsState = DataLoadingState.Success,
                        )
                    }
                is GrammarLabelsLoadResult.Failed ->
                    mutableUiState.update {
                        it.copy(
                            grammarLabelsState =
                                DataLoadingState.Error(
                                    result.cause ?: IllegalStateException("grammar labels load failed"),
                                ),
                        )
                    }
            }
        }
    }

    public fun load() {
        logicScope.launch {
            mutableUiState.update { it.copy(loadingState = DataLoadingState.Loading) }
            runCatchingCancellable {
                val card = catalogRepository.loadCard(CardId(cardId))
                val lemma = catalogRepository.loadLemma(card.lemmaId)
                card to lemma
            }.onSuccess { (card, lemma) ->
                mutableUiState.update {
                    it.copy(
                        loadingState = DataLoadingState.Success,
                        card = card.toUi(),
                        lemmaText = lemma.text,
                        relatedCards =
                            lemma.relatedCards
                                .map { related -> related.toUi(isCurrent = related.id == card.id) }
                                .toPersistentList(),
                    )
                }
            }.onFailure { throwable ->
                logger.error(throwable) { "Failed to load card $cardId" }
                mutableUiState.update { it.copy(loadingState = DataLoadingState.Error(throwable)) }
            }
        }
    }
}

private fun Card.toUi(): CardDetailCardUiState =
    CardDetailCardUiState(
        id = id.value,
        lemmaId = lemmaId.value,
        headword = headword,
        translation = translation,
        contextSentence = contextSentence,
        unitType = unitType,
        grammarTags = grammarTags.toPersistentList(),
        senseSummary = senseSummary,
        explanation = explanation,
    )

private fun CardSummary.toUi(isCurrent: Boolean): RelatedCardUiState =
    RelatedCardUiState(
        id = id.value,
        headword = headword,
        unitType = unitType,
        translation = translation,
        senseSummary = senseSummary,
        isCurrent = isCurrent,
    )

private fun initialLabelsState(seed: GrammarLabels): DataLoadingState =
    if (seed === GrammarLabels.EMPTY) DataLoadingState.Idle else DataLoadingState.Success
