package app.sensee.core.observability.logging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KermitLogSinkTest {
    @Test
    fun `a short message stays a single unlabeled chunk`() {
        assertEquals(listOf("hello"), chunkLogMessage("hello"))
    }

    @Test
    fun `a long message splits into labeled parts that reconstruct the original`() {
        val message = "x".repeat(4001)

        val parts = chunkLogMessage(message)

        assertTrue(parts.size > 1, "a message over the chunk limit is split into several entries")
        val reconstructed = parts.joinToString(separator = "") { it.substringAfter(") ") }
        assertEquals(message, reconstructed, "the labeled parts reconstruct the original payload")
    }
}
