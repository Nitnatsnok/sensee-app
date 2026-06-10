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
import app.sensee.feature.practice.domain.DuePracticeRepository
import app.sensee.feature.practice.domain.PracticeReviewRepository
import app.sensee.feature.practice.domain.PracticeSessionPolicy
import app.sensee.feature.practice.domain.PracticeSessionSource
import app.sensee.feature.practice.domain.key
import app.sensee.feature.practice.presentation.api.DeckPracticeAction
import app.sensee.feature.practice.presentation.api.DeckPracticeCardUiState
import app.sensee.feature.practice.presentation.api.DeckPracticeRatingAction
import app.sensee.feature.practice.presentation.api.DeckPracticeUiState
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarLabelsLoadResult
import app.sensee.grammar.domain.GrammarLabelsProvider
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.model.ReviewRating
import app.sensee.srs.core.model.SrsCardSnapshot
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

@AssistedInject
public class DeckPracticeLogic(
    @Assisted private val source: PracticeSessionSource,
    private val catalogRepository: CatalogRepository,
    private val reviewRepository: PracticeReviewRepository,
    private val duePracticeRepository: DuePracticeRepository,
    private val grammarLabelsProvider: GrammarLabelsProvider,
    private val clock: Clock,
    appDispatchers: AppDispatchers,
    appDiagnostics: AppDiagnostics,
) : BaseLogic(appDispatchers, appDiagnostics) {
    @AssistedFactory
    public fun interface Factory {
        public fun create(source: PracticeSessionSource): DeckPracticeLogic
    }

    public val uiState: StateFlow<DeckPracticeUiState>
        field =
        grammarLabelsProvider.cachedLabels().let { initial ->
            MutableStateFlow(
                DeckPracticeUiState(
                    grammarLabels = initial,
                    grammarLabelsState = initialLabelsState(initial),
                ),
            )
        }

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
            uiState.update { it.copy(grammarLabelsState = DataLoadingState.Loading) }
            when (val result = grammarLabelsProvider.awaitLabels()) {
                is GrammarLabelsLoadResult.Loaded ->
                    uiState.update {
                        it.copy(
                            grammarLabels = result.labels,
                            grammarLabelsState = DataLoadingState.Success,
                        )
                    }
                is GrammarLabelsLoadResult.Failed ->
                    uiState.update {
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
                uiState.update { it.copy(tapToFlipEnabled = !it.tapToFlipEnabled) }
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
            uiState.update { it.copy(loadingState = DataLoadingState.Loading) }
            runCatchingCancellable { loadSession() }
                .onSuccess { session ->
                    presentationCounts.clear()
                    session.cards.forEach { presentationCounts[it.id.value] = 0 }
                    uiState.update {
                        it.copy(
                            loadingState = DataLoadingState.Success,
                            deckTitle = session.title,
                            cards =
                                session.cards
                                    .map { card -> card.toPresentation(presentationIndex = 0) }
                                    .toPersistentList(),
                            completedCount = 0,
                        )
                    }
                }.onFailure { throwable ->
                    logger.error(throwable) { "Failed to load practice session ${source.key}" }
                    uiState.update { it.copy(loadingState = DataLoadingState.Error(throwable)) }
                }
        }
    }

    // The only source-specific step: where the initial card list comes from. The
    // queue projection, reinjection and FSRS scheduling below are identical for any
    // source. A Due source with nothing due loads an empty session (Success, not Error).
    private suspend fun loadSession(): LoadedSession =
        when (source) {
            is PracticeSessionSource.Deck -> {
                val deckWithCards = catalogRepository.loadDeck(DeckId(source.deckId))
                LoadedSession(title = deckWithCards.deck.title, cards = deckWithCards.cards)
            }

            PracticeSessionSource.Due -> {
                // Cap the batch (PracticeSessionPolicy.DUE_SESSION_LIMIT); the Home widget caps its
                // count to the same limit, so the two stay consistent even on a large backlog.
                val dueIds = duePracticeRepository.dueCardIds(clock.now(), PracticeSessionPolicy.DUE_SESSION_LIMIT)
                // No deck title for a due session — the screen's top bar is icon-only.
                LoadedSession(title = "", cards = catalogRepository.loadCards(dueIds.map { CardId(it.value) }))
            }
        }

    private data class LoadedSession(
        val title: String,
        val cards: List<Card>,
    )

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
                            cardId = SrsCardId(cardId),
                            rating = rating.toReviewRating(),
                        ),
                    )
                }.onSuccess { outcome ->
                    val shownCount = (presentationCounts[cardId] ?: 0) + 1
                    presentationCounts[cardId] = shownCount
                    val reinject = PracticeSessionPolicy.shouldReinject(outcome.srs, shownCount)
                    uiState.update { state ->
                        state.withReviewedCard(
                            cardId = cardId,
                            srs = outcome.srs,
                            shownCount = shownCount,
                            reinject = reinject,
                        )
                    }
                }.onFailure { throwable ->
                    logger.error(throwable) { "Failed to submit review for card $cardId" }
                    reportReviewFailure(throwable, cardId, rating)
                    uiState.update { it.copy(loadingState = DataLoadingState.Error(throwable)) }
                }
            } finally {
                inFlightReviews.remove(cardId)
            }
        }
    }

    private fun DeckPracticeUiState.withReviewedCard(
        cardId: String,
        srs: SrsCardSnapshot,
        shownCount: Int,
        reinject: Boolean,
    ): DeckPracticeUiState {
        val reviewedIndex = cards.indexOfFirst { it.id == cardId }
        val reviewedCard = cards.getOrNull(reviewedIndex)
        val queueAfterRemoval =
            if (reviewedIndex >= 0) {
                cards.removeAt(reviewedIndex)
            } else {
                cards
            }
        val nextQueue =
            if (reinject && reviewedCard != null) {
                queueAfterRemoval.reinjectCard(reviewedCard, srs, shownCount)
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
        reviewedCard: DeckPracticeCardUiState,
        srs: SrsCardSnapshot,
        shownCount: Int,
    ): PersistentList<DeckPracticeCardUiState> {
        val returningToEmptyDeck = isEmpty()
        val insertAt = PracticeSessionPolicy.reinjectionGap(srs).coerceAtMost(size)
        return add(
            insertAt,
            reviewedCard.reshow(
                presentationIndex = shownCount,
                animateEntrance = returningToEmptyDeck,
            ),
        )
    }

    // Re-show the card already in the session queue at a new presentation index:
    // only the key, the alternating front and the entrance animation change — the
    // display content does not, so there is no need to re-read the Card.
    private fun DeckPracticeCardUiState.reshow(
        presentationIndex: Int,
        animateEntrance: Boolean,
    ): DeckPracticeCardUiState =
        copy(
            presentationKey = "$id#$presentationIndex",
            practiceFront = PracticeSessionPolicy.frontFor(id, presentationIndex),
            animateEntrance = animateEntrance,
        )

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
                    "session" to source.key,
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
