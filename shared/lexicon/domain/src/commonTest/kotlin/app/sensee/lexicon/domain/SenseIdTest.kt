package app.sensee.lexicon.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SenseIdTest {
    @Test
    fun `a blank id is rejected`() {
        assertFailsWith<IllegalArgumentException> { SenseId(" ") }
    }

    @Test
    fun `a colon is rejected so the SRS form-key stays parseable`() {
        assertFailsWith<IllegalArgumentException> { SenseId("svc:deck:1") }
    }

    @Test
    fun `a plain id is accepted`() {
        assertEquals("p-1", SenseId("p-1").value)
    }
}
