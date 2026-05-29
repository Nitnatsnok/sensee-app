package app.sensee.feature.practice.presentation.impl.deck

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.decompose.logic.BaseLogic
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.library.domain.Card
import app.sensee.feature.library.domain.CardId
import app.sensee.feature.library.domain.CatalogRepository
import app.sensee.feature.library.domain.DeckId
import app.sensee.feature.practice.domain.CardReview
import app.sensee.feature.practice.domain.PracticeReviewRepository
import app.sensee.feature.practice.domain.PracticeSessionPolicy
import app.sensee.feature.practice.presentation.api.DeckPracticeAction
import app.sensee.feature.practice.presentation.api.DeckPracticeCardUiState
import app.sensee.feature.practice.presentation.api.DeckPracticeRatingAction
import app.sensee.feature.practice.presentation.api.DeckPracticeUiState
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarLabelsLoadResult
import app.sensee.grammar.domain.GrammarLabelsProvider
import app.sensee.srs.core.model.ReviewRating
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AssistedInject
public class DeckPracticeLogic(
    @Assisted private val deckId: String,
    private val catalogRepository: CatalogRepository,
    private val reviewRepository: PracticeReviewRepository,
    private val grammarLabelsProvider: GrammarLabelsProvider,
    appDispatchers: AppDispatchers,
    appDiagnostics: AppDiagnostics,
) : BaseLogic(appDispatchers, appDiagnostics) {
    @AssistedFactory
    public fun interface Factory {
        public fun create(deckId: String): DeckPracticeLogic
    }

    private val mutableUiState =
        grammarLabelsProvider.cachedLabels().let { initial ->
            MutableStateFlow(
                DeckPracticeUiState(
                    grammarLabels = initial,
                    grammarLabelsState = initialLabelsState(initial),
                ),
            )
        }

    public val uiState: StateFlow<DeckPracticeUiState> = mutableUiState.asStateFlow()

    // How many times each card has been *shown* this session. Drives the alternating
    // EN <-> RU direction and the safety cap below. Mutated only from `logicScope`
    // (main.immediate, single-threaded), so a plain map is safe.
    private val presentationCounts = mutableMapOf<String, Int>()

    // Guards duplicate SRS writes when a second swipe lands before the first
    // review resolves. Same single-threaded `logicScope` confinement as above.
    private val inFlightReviews = mutableSetOf<String>()

    init {
        load()
        loadGrammarLabels()
    }

    private fun retryGrammarLabels() = loadGrammarLabels()

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

    public fun onAction(action: DeckPracticeAction) {
        when (action) {
            DeckPracticeAction.Retry -> load()
            DeckPracticeAction.RetryGrammarLabels -> retryGrammarLabels()
            DeckPracticeAction.ToggleTapToFlip ->
                mutableUiState.update { it.copy(tapToFlipEnabled = !it.tapToFlipEnabled) }
            is DeckPracticeAction.SubmitReview -> submitReview(action.cardId, action.rating)
            is DeckPracticeAction.FocusCard,
            DeckPracticeAction.DismissDetails,
            is DeckPracticeAction.SpeakText,
            DeckPracticeAction.Close,
            DeckPracticeAction.OpenHelp,
            -> Unit
        }
    }

    private fun load() {
        logicScope.launch {
            mutableUiState.update { it.copy(loadingState = DataLoadingState.Loading) }
            runCatchingCancellable { catalogRepository.loadDeck(DeckId(deckId)) }
                .onSuccess { deckWithCards ->
                    presentationCounts.clear()
                    deckWithCards.cards.forEach { presentationCounts[it.id.value] = 0 }
                    mutableUiState.update {
                        it.copy(
                            loadingState = DataLoadingState.Success,
                            deckTitle = deckWithCards.deck.title,
                            cards =
                                deckWithCards.cards
                                    .map { card -> card.toPresentation(presentationIndex = 0) }
                                    .toPersistentList(),
                            completedCount = 0,
                        )
                    }
                }.onFailure { throwable ->
                    logger.error(throwable) { "Failed to load deck $deckId" }
                    mutableUiState.update { it.copy(loadingState = DataLoadingState.Error(throwable)) }
                }
        }
    }

    private fun submitReview(
        cardId: String,
        rating: DeckPracticeRatingAction,
    ) {
        if (!inFlightReviews.add(cardId)) return
        logicScope.launch {
            try {
                runCatchingCancellable {
                    reviewRepository.submitReview(
                        CardReview(
                            cardId = CardId(cardId),
                            rating = rating.toReviewRating(),
                        ),
                    )
                }.onSuccess { reviewedCard ->
                    val shownCount = (presentationCounts[reviewedCard.id.value] ?: 0) + 1
                    presentationCounts[reviewedCard.id.value] = shownCount
                    val reinject = PracticeSessionPolicy.shouldReinject(reviewedCard.srs, shownCount)
                    mutableUiState.update { state ->
                        state.withReviewedCard(
                            reviewedCard = reviewedCard,
                            shownCount = shownCount,
                            reinject = reinject,
                        )
                    }
                }.onFailure { throwable ->
                    logger.error(throwable) { "Failed to submit review for card $cardId" }
                    reportReviewFailure(throwable, cardId, rating)
                    mutableUiState.update { it.copy(loadingState = DataLoadingState.Error(throwable)) }
                }
            } finally {
                inFlightReviews.remove(cardId)
            }
        }
    }

    private fun DeckPracticeUiState.withReviewedCard(
        reviewedCard: Card,
        shownCount: Int,
        reinject: Boolean,
    ): DeckPracticeUiState {
        val reviewedIndex = cards.indexOfFirst { it.id == reviewedCard.id.value }
        val queueAfterRemoval =
            if (reviewedIndex >= 0) {
                cards.removeAt(reviewedIndex)
            } else {
                cards
            }
        val nextQueue =
            if (reinject) {
                queueAfterRemoval.reinjectCard(reviewedCard, shownCount)
            } else {
                queueAfterRemoval
            }
        return copy(
            cards = nextQueue,
            // "Cards closed": counts a card once, when FSRS graduates it
            // out of the session. Reinjections never advance it; a related card
            // injected mid-session counts when it graduates.
            completedCount = completedCount + if (reinject) 0 else 1,
            loadingState = DataLoadingState.Success,
        )
    }

    private fun PersistentList<DeckPracticeCardUiState>.reinjectCard(
        reviewedCard: Card,
        shownCount: Int,
    ): PersistentList<DeckPracticeCardUiState> {
        val returningToEmptyDeck = isEmpty()
        val insertAt = PracticeSessionPolicy.reinjectionGap(reviewedCard.srs).coerceAtMost(size)
        return add(
            insertAt,
            reviewedCard.toPresentation(
                presentationIndex = shownCount,
                animateEntrance = returningToEmptyDeck,
            ),
        )
    }

    private fun reportReviewFailure(
        throwable: Throwable,
        cardId: String,
        rating: DeckPracticeRatingAction,
    ) {
        crashReporter.recordException(
            throwable,
            attributes =
                mapOf(
                    "area" to "practice",
                    "operation" to "submit_review",
                    "deck_id" to deckId,
                    "card_id" to cardId,
                    "rating" to rating.name,
                ),
        )
    }
}

private fun Card.toPresentation(
    presentationIndex: Int,
    animateEntrance: Boolean = false,
): DeckPracticeCardUiState =
    DeckPracticeCardUiState(
        presentationKey = "${id.value}#$presentationIndex",
        id = id.value,
        lemmaId = lemmaId.value,
        headword = headword,
        translation = translation,
        contextSentence = contextSentence,
        unitType = unitType,
        grammarTags = grammarTags.toPersistentList(),
        senseSummary = senseSummary,
        explanation = explanation,
        practiceFront = PracticeSessionPolicy.frontFor(id.value, presentationIndex),
        animateEntrance = animateEntrance,
    )

private fun DeckPracticeRatingAction.toReviewRating(): ReviewRating =
    when (this) {
        DeckPracticeRatingAction.Again -> ReviewRating.Again
        DeckPracticeRatingAction.Hard -> ReviewRating.Hard
        DeckPracticeRatingAction.Good -> ReviewRating.Good
        DeckPracticeRatingAction.Easy -> ReviewRating.Easy
    }

// Cache hot at construction → skip the Loading flash on the first frame.
private fun initialLabelsState(seed: GrammarLabels): DataLoadingState =
    if (seed === GrammarLabels.EMPTY) DataLoadingState.Idle else DataLoadingState.Success
