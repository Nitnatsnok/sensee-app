package app.sensee.feature.practice.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PracticeSessionSourceTest {
    @Test
    fun `key is stable and distinct per source`() {
        assertEquals("deck:d1", PracticeSessionSource.Deck("d1").key)
        assertEquals("due", PracticeSessionSource.Due.key)
        assertNotEquals(
            PracticeSessionSource.Deck("d1").key,
            PracticeSessionSource.Deck("d2").key,
            "a different deck yields a different key",
        )
        assertNotEquals(
            PracticeSessionSource.Deck("due").key,
            PracticeSessionSource.Due.key,
            "a deck named 'due' does not collide with the due source",
        )
    }
}
