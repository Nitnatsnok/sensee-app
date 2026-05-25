package app.sensee.grammar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SentenceSegmentTest {
    @Test
    fun `a marked target splits into text and target segments`() {
        val sentence = StudiedSentence.parse("She [[came across]] the letters.")

        assertEquals(
            listOf(
                SentenceSegment.Text("She "),
                SentenceSegment.Target("came across"),
                SentenceSegment.Text(" the letters."),
            ),
            sentence.segments,
        )
        assertEquals("came across", sentence.target)
        assertEquals("She came across the letters.", sentence.plainText())
    }

    @Test
    fun `an unmarked sentence is a single text segment with no target`() {
        val sentence = StudiedSentence.parse("No target here.")

        assertEquals(listOf(SentenceSegment.Text("No target here.")), sentence.segments)
        assertNull(sentence.target)
        assertEquals("No target here.", sentence.plainText())
    }

    @Test
    fun `marked is the inverse of parse - round-trips the target span`() {
        val raw = "She [[came across]] the letters."

        assertEquals(raw, StudiedSentence.parse(raw).marked())
    }

    @Test
    fun `a sentence needs at least one segment`() {
        assertFailsWith<IllegalArgumentException> { StudiedSentence(emptyList()) }
    }

    @Test
    fun `every marked span becomes its own target segment`() {
        val sentence = StudiedSentence.parse("She [[comes across]] [[as]] very confident.")

        assertEquals(
            listOf(
                SentenceSegment.Text("She "),
                SentenceSegment.Target("comes across"),
                SentenceSegment.Text(" "),
                SentenceSegment.Target("as"),
                SentenceSegment.Text(" very confident."),
            ),
            sentence.segments,
        )
        assertEquals("She comes across as very confident.", sentence.plainText())
    }

    @Test
    fun `marked round-trips a sentence with multiple targets`() {
        val raw = "She [[comes across]] [[as]] very confident."

        assertEquals(raw, StudiedSentence.parse(raw).marked())
    }

    @Test
    fun `an unbalanced opening marker keeps the tail as plain text`() {
        val sentence = StudiedSentence.parse("She came [[across the room.")

        assertEquals(
            listOf(SentenceSegment.Text("She came [[across the room.")),
            sentence.segments,
        )
    }
}
