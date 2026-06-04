package app.sensee.lexicon.domain

import app.sensee.grammar.domain.SurfaceForm
import kotlin.test.Test
import kotlin.test.assertEquals

class LemmaKeyTest {
    @Test
    fun `the head lemma wins and is lowercased`() {
        val sense = Sense(translation = "t", headLemma = "Come", surfaceForm = SurfaceForm.parse("come across"))

        assertEquals("come", deriveLemmaKey(sense))
    }

    @Test
    fun `it falls back to the surface form when no lemma is resolved`() {
        val sense = Sense(translation = "t", surfaceForm = SurfaceForm.parse("Come Across"))

        assertEquals("come across", deriveLemmaKey(sense))
    }

    @Test
    fun `it falls back to the translation for a bare draft`() {
        val sense = Sense(translation = "Наткнуться")

        assertEquals("наткнуться", deriveLemmaKey(sense))
    }
}
