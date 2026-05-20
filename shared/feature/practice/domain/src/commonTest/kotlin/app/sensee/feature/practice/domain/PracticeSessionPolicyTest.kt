package app.sensee.feature.practice.domain

import app.sensee.srs.core.model.SrsCardState
import app.sensee.srs.testKit.SrsTestCards
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class PracticeSessionPolicyTest {
    @Test
    fun `a card still in learning is reinjected into the session`() {
        assertTrue(PracticeSessionPolicy.shouldReinject(SrsTestCards.learningCard(), shownCount = 1))
    }

    @Test
    fun `a card back in relearning is reinjected into the session`() {
        val relearning = SrsTestCards.learningCard(state = SrsCardState.Relearning)
        assertTrue(PracticeSessionPolicy.shouldReinject(relearning, shownCount = 1))
    }

    @Test
    fun `a graduated review card leaves the session`() {
        assertFalse(PracticeSessionPolicy.shouldReinject(SrsTestCards.dueReviewCard(), shownCount = 1))
    }

    @Test
    fun `new and suspended cards are never reinjected`() {
        assertFalse(PracticeSessionPolicy.shouldReinject(SrsTestCards.newCard(), shownCount = 1))
        assertFalse(PracticeSessionPolicy.shouldReinject(SrsTestCards.suspendedCard(), shownCount = 1))
    }

    @Test
    fun `a card at the presentation cap stops being reinjected even while still learning`() {
        val stillLearning = SrsTestCards.learningCard()
        assertTrue(
            PracticeSessionPolicy.shouldReinject(
                stillLearning,
                shownCount = PracticeSessionPolicy.MAX_PRESENTATIONS_PER_CARD - 1,
            ),
        )
        assertFalse(
            PracticeSessionPolicy.shouldReinject(
                stillLearning,
                shownCount = PracticeSessionPolicy.MAX_PRESENTATIONS_PER_CARD,
            ),
        )
    }

    @Test
    fun `a short learning step comes back sooner than a long one`() {
        val nearGap = PracticeSessionPolicy.reinjectionGap(SrsTestCards.learningCard(scheduledInterval = 1.minutes))
        val farGap = PracticeSessionPolicy.reinjectionGap(SrsTestCards.learningCard(scheduledInterval = 10.minutes))
        assertTrue(nearGap < farGap, "a more urgent card resurfaces closer in the queue")
    }

    @Test
    fun `the short-step boundary is inclusive`() {
        assertEquals(
            PracticeSessionPolicy.reinjectionGap(SrsTestCards.learningCard(scheduledInterval = 1.minutes)),
            PracticeSessionPolicy.reinjectionGap(SrsTestCards.learningCard(scheduledInterval = 2.minutes)),
        )
    }

    @Test
    fun `practice direction flips on every revisit of the same card`() {
        assertNotEquals(
            PracticeSessionPolicy.frontFor("card-1", presentationIndex = 0),
            PracticeSessionPolicy.frontFor("card-1", presentationIndex = 1),
        )
    }

    @Test
    fun `practice direction is deterministic for the same card and showing`() {
        assertEquals(
            PracticeSessionPolicy.frontFor("card-1", presentationIndex = 0),
            PracticeSessionPolicy.frontFor("card-1", presentationIndex = 0),
        )
    }

    @Test
    fun `different cards can start on different sides`() {
        assertNotEquals(
            PracticeSessionPolicy.frontFor("a", presentationIndex = 0),
            PracticeSessionPolicy.frontFor("b", presentationIndex = 0),
        )
    }
}
