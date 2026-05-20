package app.sensee.grammar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ComplementTypeTest {
    @Test
    fun `resolves a wire id ignoring separators and case`() {
        assertEquals(ComplementType.ToInfinitive, ComplementType.fromId("to_infinitive"))
        assertEquals(ComplementType.Gerund, ComplementType.fromId("Gerund"))
        assertEquals(ComplementType.PrepositionalPhrase, ComplementType.fromId("prepositional-phrase"))
    }

    @Test
    fun `an unknown id resolves to null - dropped at the boundary`() {
        assertNull(ComplementType.fromId("subjunctive"))
    }
}
