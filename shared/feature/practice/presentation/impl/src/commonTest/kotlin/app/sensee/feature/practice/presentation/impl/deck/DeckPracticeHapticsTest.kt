package app.sensee.feature.practice.presentation.impl.deck

import app.sensee.ui.learningDeck.LearningDeckSwipePhase
import app.sensee.ui.learningDeck.LearningDeckSwipeState
import app.sensee.ui.learningDeck.LearningSwipeDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeckPracticeHapticsTest {
    private fun dragging(
        progress: Float,
        direction: LearningSwipeDirection?,
    ): LearningDeckSwipeState =
        LearningDeckSwipeState.Idle.copy(
            phase = LearningDeckSwipePhase.Dragging,
            progress = progress,
            direction = direction,
        )

    @Test
    fun `live drag exposes progress for threshold haptics`() {
        val state = dragging(progress = 1f, direction = LearningSwipeDirection.Start)

        assertEquals(1f, swipeThresholdHapticProgress(state))
    }

    @Test
    fun `drag below the threshold still exposes progress for haptic re-arm`() {
        val state = dragging(progress = 0.5f, direction = LearningSwipeDirection.Start)

        assertEquals(0.5f, swipeThresholdHapticProgress(state))
    }

    @Test
    fun `directionless drag does not pulse`() {
        val state = dragging(progress = 1f, direction = null)

        assertNull(swipeThresholdHapticProgress(state))
    }

    @Test
    fun `idle state does not arm`() {
        assertNull(swipeThresholdHapticProgress(LearningDeckSwipeState.Idle))
    }

    @Test
    fun `dismiss animation at full progress does not pulse`() {
        val state =
            LearningDeckSwipeState.Idle.copy(
                phase = LearningDeckSwipePhase.Dismissing,
                progress = 1f,
                direction = LearningSwipeDirection.End,
            )

        assertNull(swipeThresholdHapticProgress(state))
    }
}
