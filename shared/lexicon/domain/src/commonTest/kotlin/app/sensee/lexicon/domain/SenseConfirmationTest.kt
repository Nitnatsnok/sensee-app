package app.sensee.lexicon.domain

import app.sensee.grammar.domain.StudiedSentence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SenseConfirmationTest {
    private val withExample =
        listOf(ContextualApplication(StudiedSentence.parse("I [[came across]] a photo.")))

    @Test
    fun `a sense with a translation and an example is confirmable`() {
        val sense = Sense(translation = "наткнуться", contextualApplications = withExample)

        assertTrue(sense.isConfirmable())
        assertEquals(sense, sense.requireConfirmable())
    }

    @Test
    fun `a blank translation is not confirmable`() {
        val sense = Sense(translation = "", contextualApplications = withExample)

        assertFalse(sense.isConfirmable())
        assertFailsWith<IllegalArgumentException> { sense.requireConfirmable() }
    }

    @Test
    fun `a sense with no contextual application is not confirmable`() {
        val sense = Sense(translation = "наткнуться")

        assertFalse(sense.isConfirmable())
        assertFailsWith<IllegalArgumentException> { sense.requireConfirmable() }
    }
}
