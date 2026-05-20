package app.sensee.ui.learningDeck

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LearningCardFlipStateTest {
    @Test
    fun `flip toggles card side`() {
        val state = LearningCardFlipState(initiallyBackVisible = false)

        state.flip()
        assertTrue(state.isBackVisible)

        state.flip()
        assertFalse(state.isBackVisible)
    }

    @Test
    fun `explicit side methods override current value`() {
        val state = LearningCardFlipState(initiallyBackVisible = false)

        state.showBack()
        assertTrue(state.isBackVisible)

        state.showFront()
        assertFalse(state.isBackVisible)

        state.setSide(backVisible = true)
        assertTrue(state.isBackVisible)
    }
}
