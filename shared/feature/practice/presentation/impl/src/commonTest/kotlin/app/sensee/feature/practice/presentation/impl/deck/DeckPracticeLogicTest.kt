package app.sensee.feature.practice.presentation.impl.deck

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
import app.sensee.feature.practice.domain.CardReview
import app.sensee.feature.practice.domain.DuePracticeRepository
import app.sensee.feature.practice.domain.PracticeReviewRepository
import app.sensee.feature.practice.domain.PracticeSessionPolicy
import app.sensee.feature.practice.domain.PracticeSessionSource
import app.sensee.feature.practice.domain.ReviewOutcome
import app.sensee.feature.practice.presentation.api.DeckPracticeAction
import app.sensee.feature.practice.presentation.api.DeckPracticeRatingAction
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarLabelsProvider
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.model.SrsCardSnapshot
import app.sensee.srs.testKit.SrsTestCards
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class DeckPracticeLogicTest {
    @Test
    fun `card left in learning state is reinjected later in the session`() {
        val repository = FakeDecksRepository(deckOf("c1", "c2", "c3", "c4"))
        val logic = newLogic(repository)
        val originalFront =
            logic.uiState.value.cards
                .first { it.id == "c1" }
                .practiceFront

        repository.nextSrs = learning(intervalMinutes = 1)
        logic.onAction(DeckPracticeAction.SubmitReview("c1", DeckPracticeRatingAction.Again))

        val cards = logic.uiState.value.cards
        assertEquals(4, cards.size, "card stays in the session")
        assertEquals("c2", cards.first().id, "the reinjected card is not shown again immediately")
        val reinjected = cards.single { it.id == "c1" }
        assertEquals("c1#1", reinjected.presentationKey)
        assertFalse(
            reinjected.animateEntrance,
            "no entrance animation when other cards still fill the deck",
        )
        assertNotEquals(
            originalFront,
            reinjected.practiceFront,
            "direction flips between presentations of the same card",
        )
    }

    @Test
    fun `graduated card leaves the session`() {
        val repository = FakeDecksRepository(deckOf("c1", "c2", "c3"))
        val logic = newLogic(repository)

        repository.nextSrs = review()
        logic.onAction(DeckPracticeAction.SubmitReview("c1", DeckPracticeRatingAction.Good))

        val cards = logic.uiState.value.cards
        assertEquals(2, cards.size)
        assertTrue(cards.none { it.id == "c1" }, "a graduated card does not come back")
    }

    @Test
    fun `completedCount counts a card once on graduation and never on reinjection`() {
        val repository = FakeDecksRepository(deckOf("c1", "c2"))
        val logic = newLogic(repository)

        repository.nextSrs = learning(intervalMinutes = 1)
        logic.onAction(DeckPracticeAction.SubmitReview("c1", DeckPracticeRatingAction.Again))
        assertEquals(
            0,
            logic.uiState.value.completedCount,
            "a reinjected card is not closed yet",
        )

        repository.nextSrs = review()
        logic.onAction(DeckPracticeAction.SubmitReview("c1", DeckPracticeRatingAction.Good))
        logic.onAction(DeckPracticeAction.SubmitReview("c2", DeckPracticeRatingAction.Good))

        assertEquals(2, logic.uiState.value.completedCount, "each card counts once when it graduates")
    }

    @Test
    fun `a card cannot loop forever even if always rated again`() {
        val repository = FakeDecksRepository(deckOf("only"))
        val logic = newLogic(repository)
        repository.nextSrs = learning(intervalMinutes = 1)

        repeat(PracticeSessionPolicy.MAX_PRESENTATIONS_PER_CARD) {
            assertTrue(
                logic.uiState.value.cards
                    .isNotEmpty(),
                "still drilling at attempt $it",
            )
            logic.onAction(DeckPracticeAction.SubmitReview("only", DeckPracticeRatingAction.Again))
        }

        assertTrue(
            logic.uiState.value.cards
                .isEmpty(),
            "the session ends once the per-card presentation cap is reached",
        )
    }

    @Test
    fun `cards stay non-empty until the last card's review resolves`() {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeDecksRepository(deckOf("only"), gate)
        repository.nextSrs = learning(intervalMinutes = 1)
        val logic = newLogic(repository)

        logic.onAction(DeckPracticeAction.SubmitReview("only", DeckPracticeRatingAction.Again))

        // The deck is visually empty (the swipe is recorded) but `cards` is only mutated
        // when the review resolves — staying non-empty here is what stops the screen from
        // flashing "deck finished" before the card is reinjected.
        assertFalse(
            logic.uiState.value.cards
                .isEmpty(),
        )

        gate.complete(Unit)

        val cards = logic.uiState.value.cards
        assertEquals(1, cards.size, "an Again'd card is reinjected, not finished")
        assertEquals("only#1", cards.single().presentationKey)
        assertTrue(
            cards.single().animateEntrance,
            "the last card returning to an empty deck animates its entrance",
        )
    }

    @Test
    fun `cards empty only after the last card graduates`() {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeDecksRepository(deckOf("only"), gate)
        repository.nextSrs = review()
        val logic = newLogic(repository)

        logic.onAction(DeckPracticeAction.SubmitReview("only", DeckPracticeRatingAction.Easy))
        assertFalse(
            logic.uiState.value.cards
                .isEmpty(),
            "not resolved yet",
        )

        gate.complete(Unit)

        assertTrue(
            logic.uiState.value.cards
                .isEmpty(),
        )
    }

    @Test
    fun `a second swipe on a card whose review is in flight does not submit twice`() {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeDecksRepository(deckOf("only"), gate)
        val logic = newLogic(repository)

        logic.onAction(DeckPracticeAction.SubmitReview("only", DeckPracticeRatingAction.Good))
        logic.onAction(DeckPracticeAction.SubmitReview("only", DeckPracticeRatingAction.Good))
        gate.complete(Unit)

        assertEquals(1, repository.submitCount, "the in-flight review is not re-submitted")
    }

    @Test
    fun `a due session loads the queried due cards in due order`() {
        val repository = FakeDecksRepository(deckOf("ignored"))
        repository.duePool = listOf(card("d2"), card("d1"), card("d3"))
        repository.dueIds = listOf(SrsCardId("d2"), SrsCardId("d1"), SrsCardId("d3"))
        val logic = newLogic(repository, source = PracticeSessionSource.Due)

        assertEquals(
            listOf("d2", "d1", "d3"),
            logic.uiState.value.cards
                .map { it.id },
            "a due session plays the due cards in the order the due query returned",
        )
    }

    @Test
    fun `a due session with no due cards finishes empty without an error`() {
        val repository = FakeDecksRepository(deckOf("ignored"))
        repository.dueIds = emptyList()
        val logic = newLogic(repository, source = PracticeSessionSource.Due)

        assertTrue(
            logic.uiState.value.cards
                .isEmpty(),
            "nothing is due",
        )
        assertEquals(
            DataLoadingState.Success,
            logic.uiState.value.loadingState,
            "an empty due session is a valid finished session, not an error",
        )
    }

    @Test
    fun `a due session requests at most the shared due session limit`() {
        val repository = FakeDecksRepository(deckOf("ignored"))
        repository.dueIds = listOf(SrsCardId("d1"))
        newLogic(repository, source = PracticeSessionSource.Due)

        assertEquals(
            PracticeSessionPolicy.DUE_SESSION_LIMIT,
            repository.requestedDueLimit,
            "the session caps to the same limit the Home widget counts against",
        )
    }

    @Test
    fun `a due session reviews through the single FSRS scheduler like a deck`() {
        val repository = FakeDecksRepository(deckOf("ignored"))
        repository.duePool = listOf(card("d1"), card("d2"))
        repository.dueIds = listOf(SrsCardId("d1"), SrsCardId("d2"))
        val logic = newLogic(repository, source = PracticeSessionSource.Due)

        repository.nextSrs = review()
        logic.onAction(DeckPracticeAction.SubmitReview("d1", DeckPracticeRatingAction.Good))

        val cards = logic.uiState.value.cards
        assertEquals(1, repository.submitCount, "the due card is scheduled through PracticeReviewRepository")
        assertTrue(cards.none { it.id == "d1" }, "a graduated due card leaves the session, same as a deck card")
    }

    private fun newLogic(
        repository: FakeDecksRepository,
        source: PracticeSessionSource = PracticeSessionSource.Deck("deck"),
    ): DeckPracticeLogic =
        DeckPracticeLogic(
            source = source,
            catalogRepository = repository,
            reviewRepository = repository,
            duePracticeRepository = repository,
            grammarLabelsProvider = GrammarLabelsProvider { GrammarLabels.EMPTY },
            clock = FixedClock,
            appDispatchers = immediateAppDispatchers(),
            appDiagnostics = noOpAppDiagnostics(),
        )

    // When [gate] is supplied, `submitReview` suspends on it so a test can observe state
    // while a review is in flight; otherwise reviews resolve synchronously.
    private class FakeDecksRepository(
        private val deck: DeckWithCards,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : CatalogRepository,
        PracticeReviewRepository,
        DuePracticeRepository {
        var nextSrs: SrsCardSnapshot = learning(intervalMinutes = 1)
        var submitCount = 0

        // Due-source inputs: the ids the due query returns and the card pool to resolve them.
        var dueIds: List<SrsCardId> = emptyList()
        var duePool: List<Card> = emptyList()

        // The limit the logic asked the due query for — pins it to the shared session cap.
        var requestedDueLimit: Int? = null
            private set

        override fun observeOwnedMaterial(): Flow<List<Deck>> = flowOf(listOf(deck.deck))

        override suspend fun refreshFromRemote() = Unit

        override suspend fun loadDeck(deckId: DeckId): DeckWithCards = deck

        override suspend fun previewDeck(deckId: DeckId): DeckWithCards = deck

        override suspend fun loadCard(cardId: CardId): Card = deck.cards.first { it.id == cardId }

        override suspend fun loadCards(cardIds: List<CardId>): List<Card> {
            val pool = (deck.cards + duePool).associateBy { it.id }
            return cardIds.mapNotNull { pool[it] }
        }

        override suspend fun loadLemma(lemmaId: LemmaId): Lemma = Lemma(lemmaId, lemmaId.value, emptyList())

        override suspend fun submitReview(review: CardReview): ReviewOutcome {
            submitCount++
            gate?.await()
            return ReviewOutcome(srs = nextSrs)
        }

        override suspend fun countDue(now: Instant): Int = dueIds.size

        override fun observeDueCount(now: Instant): Flow<Int> = flowOf(dueIds.size)

        override suspend fun dueCardIds(
            now: Instant,
            limit: Int,
        ): List<SrsCardId> {
            requestedDueLimit = limit
            return dueIds.take(limit)
        }
    }

    private object FixedClock : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(0L)
    }

    private companion object {
        fun deckOf(vararg ids: String): DeckWithCards =
            DeckWithCards(
                deck =
                    Deck(
                        id = DeckId("deck"),
                        title = "Deck",
                        description = "",
                        cardCount = ids.size,
                        origin = CatalogOrigin.Service,
                    ),
                cards = ids.map(::card),
            )

        fun card(id: String): Card =
            Card(
                id = CardId(id),
                lemmaId = LemmaId("lemma-$id"),
                headword = id,
                translation = "перевод-$id",
                contextSentence = "sentence $id",
                unitType = GrammarUnitType.Verb,
                grammarTags = emptyList(),
                senseSummary = "sense $id",
                explanation = "explanation $id",
            )

        fun learning(intervalMinutes: Int): SrsCardSnapshot =
            SrsTestCards.learningCard(scheduledInterval = intervalMinutes.minutes)

        fun review(): SrsCardSnapshot = SrsTestCards.dueReviewCard(scheduledInterval = 4.days)
    }
}
