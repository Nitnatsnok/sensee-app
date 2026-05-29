package app.sensee.verification.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SentenceHintTest {
    @Test
    fun `interleaved Target segments survive plain rendering`() {
        val sentence =
            SentenceHint(
                listOf(
                    SentenceHint.Segment.Text("He "),
                    SentenceHint.Segment.Target("turn"),
                    SentenceHint.Segment.Text(" it "),
                    SentenceHint.Segment.Target("on"),
                    SentenceHint.Segment.Text("."),
                ),
            )

        assertEquals("He turn it on.", sentence.plainText())
        assertEquals(listOf("turn", "on"), sentence.targets.map { it.value })
        assertEquals(listOf(1, 3), sentence.targetSegmentIndices)
        assertEquals("turn on", sentence.studiedUnitDisplay())
    }

    @Test
    fun `a sentence with no segments is rejected at construction time`() {
        assertFailsWith<IllegalArgumentException> { SentenceHint(emptyList()) }
    }
}
