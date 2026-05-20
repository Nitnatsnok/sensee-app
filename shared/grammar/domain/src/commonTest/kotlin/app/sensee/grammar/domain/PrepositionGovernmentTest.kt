package app.sensee.grammar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PrepositionGovernmentTest {
    @Test
    fun `interchangeable prepositions are grouped with an optional example`() {
        val group =
            PrepositionGovernment(
                alternatives = listOf("from", "to", "than"),
                example = "Her view is different from mine.",
            )

        assertEquals(listOf("from", "to", "than"), group.alternatives)
        assertEquals("Her view is different from mine.", group.example)
    }

    @Test
    fun `a single governed preposition needs no example`() {
        val group = PrepositionGovernment(alternatives = listOf("on"))

        assertEquals(listOf("on"), group.alternatives)
        assertNull(group.example)
    }

    @Test
    fun `a group needs at least one preposition`() {
        assertFailsWith<IllegalArgumentException> { PrepositionGovernment(emptyList()) }
    }
}
