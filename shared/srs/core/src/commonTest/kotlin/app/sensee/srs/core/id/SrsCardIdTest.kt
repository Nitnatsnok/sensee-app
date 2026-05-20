package app.sensee.srs.core.id

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SrsCardIdTest {
    @Test
    fun `creates id from non blank value`() {
        val id = SrsCardId("card-1")

        assertEquals("card-1", id.value)
    }

    @Test
    fun `throws for blank value`() {
        assertFailsWith<IllegalArgumentException> {
            SrsCardId(" ")
        }
    }
}
