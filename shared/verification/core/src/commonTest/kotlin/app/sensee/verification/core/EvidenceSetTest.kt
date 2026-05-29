package app.sensee.verification.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EvidenceSetTest {
    @Test
    fun `consensus is the shared value when every observation agrees`() {
        val set =
            EvidenceSet(
                listOf(
                    Observation(LexicalExistence.Confirmed, ref("free-dictionary"), Confidence.Medium),
                    Observation(LexicalExistence.Confirmed, ref("datamuse"), Confidence.High),
                ),
            )

        assertEquals(LexicalExistence.Confirmed, set.consensus)
        assertFalse(set.hasConflict)
    }

    @Test
    fun `consensus is null when sources disagree and the conflict flag surfaces`() {
        val set =
            EvidenceSet(
                listOf(
                    Observation(LexicalExistence.Confirmed, ref("free-dictionary"), Confidence.Medium),
                    Observation(LexicalExistence.NotFound, ref("free-dict"), Confidence.Low),
                ),
            )

        assertNull(set.consensus)
        assertTrue(set.hasConflict)
    }

    @Test
    fun `an empty evidence set has no consensus and no conflict`() {
        val set = EvidenceSet.empty<LexicalExistence>()

        assertNull(set.consensus)
        assertFalse(set.hasConflict)
        assertEquals(emptyList(), set.observations)
    }

    private fun ref(sourceId: String): LexicalSourceRef =
        LexicalSourceRef(sourceId = sourceId, fetchedAtEpochMillis = 0L)
}
