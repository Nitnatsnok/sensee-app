package app.sensee.grammar.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ComponentRoleTest {
    @Test
    fun `resolves a wire id ignoring separators and case`() {
        assertEquals(ComponentRole.Head, ComponentRole.fromId("head"))
        assertEquals(ComponentRole.FixedObject, ComponentRole.fromId("Fixed_Object"))
        assertEquals(ComponentRole.Preposition, ComponentRole.fromId("PREPOSITION"))
    }

    @Test
    fun `an unknown id surfaces as Unknown carrying the original wire string`() {
        val resolved = ComponentRole.fromId("connective")

        assertEquals(ComponentRole.Unknown("connective"), resolved)
    }
}
