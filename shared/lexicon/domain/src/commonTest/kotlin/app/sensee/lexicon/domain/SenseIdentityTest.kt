package app.sensee.lexicon.domain

import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.SurfaceForm
import kotlin.test.Test
import kotlin.test.assertEquals

class SenseIdentityTest {
    private val base =
        Sense(
            translation = "наткнуться",
            surfaceForm = SurfaceForm.parse("come across"),
            unitType = GrammarUnitType.PhrasalVerb,
        )

    @Test
    fun `content key ignores cefr`() {
        assertEquals(
            deriveSenseContentKey(base),
            deriveSenseContentKey(base.copy(cefr = CefrLevel.B2)),
        )
    }

    @Test
    fun `content key is case insensitive on surface form and translation`() {
        val upper = base.copy(translation = base.translation.uppercase())
        assertEquals(deriveSenseContentKey(base), deriveSenseContentKey(upper))
    }

    @Test
    fun `content key separates senses by translation`() {
        val other = base.copy(translation = "произвести впечатление")
        assertEquals(false, deriveSenseContentKey(base) == deriveSenseContentKey(other))
    }
}
