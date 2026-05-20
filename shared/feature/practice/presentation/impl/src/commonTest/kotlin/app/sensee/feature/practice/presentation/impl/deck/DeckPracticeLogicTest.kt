package app.sensee.feature.practice.presentation.impl.deck

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
import app.sensee.feature.practice.domain.PracticeReviewRepository
import app.sensee.feature.practice.domain.PracticeSessionPolicy
import app.sensee.feature.practice.presentation.api.DeckPracticeAction
import app.sensee.feature.practice.presentation.api.DeckPracticeRatingAction
import app.sensee.grammar.domain.GrammarUnitType
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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

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

    private fun newLogic(repository: FakeDecksRepository): DeckPracticeLogic =
        DeckPracticeLogic(
            deckId = "deck",
            catalogRepository = repository,
            reviewRepository = repository,
            appDispatchers = immediateAppDispatchers(),
            appDiagnostics = noOpAppDiagnostics(),
        )

    // When [gate] is supplied, `submitReview` suspends on it so a test can observe state
    // while a review is in flight; otherwise reviews resolve synchronously.
    private class FakeDecksRepository(
        private val deck: DeckWithCards,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : CatalogRepository,
        PracticeReviewRepository {
        var nextSrs: SrsCardSnapshot = learning(intervalMinutes = 1)
        var submitCount = 0

        override fun observeOwnedMaterial(): Flow<List<Deck>> = flowOf(listOf(deck.deck))

        override suspend fun refreshFromRemote() = Unit

        override suspend fun loadDeck(deckId: DeckId): DeckWithCards = deck

        override suspend fun loadCard(cardId: CardId): Card = deck.cards.first { it.id == cardId }

        override suspend fun loadLemma(lemmaId: LemmaId): Lemma = Lemma(lemmaId, lemmaId.value, emptyList())

        override suspend fun submitReview(review: CardReview): Card {
            submitCount++
            gate?.await()
            return deck.cards.first { it.id == review.cardId }.copy(srs = nextSrs)
        }
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
                srs = SrsTestCards.newCard(id),
            )

        fun learning(intervalMinutes: Int): SrsCardSnapshot =
            SrsTestCards.learningCard(scheduledInterval = intervalMinutes.minutes)

        fun review(): SrsCardSnapshot = SrsTestCards.dueReviewCard(scheduledInterval = 4.days)
    }
}
