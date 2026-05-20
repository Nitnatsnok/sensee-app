package app.sensee.grammar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SurfaceFormTest {
    @Test
    fun `an optional particle renders in square brackets`() {
        val form =
            SurfaceForm(
                listOf(
                    SurfaceToken.Literal("come across"),
                    SurfaceToken.Optional("as"),
                ),
            )

        assertEquals("come across [as]", form.display())
    }

    @Test
    fun `a slot renders in angle brackets`() {
        val form =
            SurfaceForm(
                listOf(
                    SurfaceToken.Literal("come across"),
                    SurfaceToken.Slot("something"),
                ),
            )

        assertEquals("come across <something>", form.display())
    }

    @Test
    fun `a surface form needs at least one token`() {
        assertFailsWith<IllegalArgumentException> { SurfaceForm(emptyList()) }
    }

    @Test
    fun `parsing keeps literals and optionals and slots distinct`() {
        val form = SurfaceForm.parse("come across [as] <something>")

        assertEquals(
            listOf(
                SurfaceToken.Literal("come across"),
                SurfaceToken.Optional("as"),
                SurfaceToken.Slot("something"),
            ),
            form.tokens,
        )
        assertEquals("come across [as] <something>", form.display())
    }
}
