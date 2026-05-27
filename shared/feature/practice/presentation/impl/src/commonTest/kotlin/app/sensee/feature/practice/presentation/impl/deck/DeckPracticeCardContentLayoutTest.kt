package app.sensee.feature.practice.presentation.impl.deck

import androidx.compose.ui.unit.Constraints
import kotlin.test.Test
import kotlin.test.assertEquals

class DeckPracticeCardContentLayoutTest {
    @Test
    fun `speakable slot clamps text min height to compressed parent`() {
        val textConstraints =
            speakableSlotTextConstraints(
                constraints = Constraints(maxWidth = 120, maxHeight = 24),
                maxWidth = 96,
                minHeight = 48,
            )

        assertEquals(0, textConstraints.minWidth)
        assertEquals(96, textConstraints.maxWidth)
        assertEquals(24, textConstraints.minHeight)
        assertEquals(24, textConstraints.maxHeight)
    }

    @Test
    fun `speakable slot keeps text min height when parent has room`() {
        val textConstraints =
            speakableSlotTextConstraints(
                constraints = Constraints(maxWidth = 120, maxHeight = 80),
                maxWidth = 96,
                minHeight = 48,
            )

        assertEquals(48, textConstraints.minHeight)
        assertEquals(80, textConstraints.maxHeight)
    }
}
