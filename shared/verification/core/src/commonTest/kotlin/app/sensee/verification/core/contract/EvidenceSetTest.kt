package app.sensee.verification.core.contract

import app.sensee.verification.core.grounding.CefrLevel
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

    @Test
    fun `nullable T agreed null is indistinguishable from no consensus by design`() {
        // Pins the KDoc on [EvidenceSet.consensus]: for a nullable `T` an
        // agreed `null` (every source said "I don't know") and "no observations"
        // both surface as `consensus == null`. Callers depend on `hasConflict`
        // to disambiguate: `consensus == null && hasConflict == true` means
        // sources disagree; `consensus == null && hasConflict == false`
        // means either everyone said null OR no source ran. Many report fields
        // are EvidenceSet<T?>; a regression here would silently merge those
        // two states into one.
        val allNull =
            EvidenceSet<CefrLevel?>(
                listOf(
                    Observation(null, ref("free-dictionary"), Confidence.Medium),
                    Observation(null, ref("datamuse"), Confidence.Low),
                ),
            )

        assertNull(allNull.consensus)
        assertFalse(allNull.hasConflict, "agreed-null must NOT flag as conflict")
        assertEquals(2, allNull.observations.size, "the all-null agreement is still attributed evidence")
    }

    private fun ref(sourceId: String): LexicalSourceRef =
        LexicalSourceRef(sourceId = sourceId, fetchedAtEpochMillis = 0L)
}
