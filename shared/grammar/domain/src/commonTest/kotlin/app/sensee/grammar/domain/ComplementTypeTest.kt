package app.sensee.grammar.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ComplementTypeTest {
    @Test
    fun `resolves a wire id ignoring separators and case`() {
        assertEquals(ComplementType.ToInfinitive, ComplementType.fromId("to_infinitive"))
        assertEquals(ComplementType.Gerund, ComplementType.fromId("Gerund"))
        assertEquals(ComplementType.PrepositionalPhrase, ComplementType.fromId("prepositional-phrase"))
    }

    @Test
    fun `an unknown id surfaces as Unknown carrying the original wire string`() {
        val resolved = ComplementType.fromId("subjunctive")

        assertEquals(ComplementType.Unknown("subjunctive"), resolved)
    }
}
