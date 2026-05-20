package app.sensee.feature.practice.data.local

import app.sensee.core.database.Practice_srs_card
import app.sensee.srs.core.model.SrsCardState
import app.sensee.srs.fsrs.FsrsAlgorithmState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class PracticeSrsMappersTest {
    @Test
    fun `a fully populated row maps every column onto the snapshot`() {
        val snapshot =
            row(
                state = "Review",
                dueAtEpochMs = 1_000L,
                lastReviewedAtEpochMs = 500L,
                scheduledIntervalMs = 86_400_000L,
                reviewCount = 7L,
                lapseCount = 2L,
                stepIndex = 1L,
                algorithmName = "fsrs",
                algorithmVersion = "v6",
                fsrsDifficulty = 5.0,
                fsrsStability = 6.0,
                parametersId = "params-1",
            ).toSrsCardSnapshot()

        assertEquals("card-1", snapshot.id.value)
        assertEquals(SrsCardState.Review, snapshot.state)
        assertEquals(Instant.fromEpochMilliseconds(1_000L), snapshot.dueAt)
        assertEquals(Instant.fromEpochMilliseconds(500L), snapshot.lastReviewedAt)
        assertEquals(86_400_000L.milliseconds, snapshot.scheduledInterval)
        assertEquals(7, snapshot.reviewCount)
        assertEquals(2, snapshot.lapseCount)
        assertEquals(1, snapshot.stepIndex)
        assertEquals("params-1", snapshot.parametersId?.value)
        assertEquals("fsrs", snapshot.algorithm?.name)
        assertEquals("v6", snapshot.algorithm?.version)

        val algorithmState = assertIs<FsrsAlgorithmState>(snapshot.algorithmState)
        assertEquals(5.0, algorithmState.difficulty)
        assertEquals(6.0, algorithmState.stability)
        assertEquals(
            snapshot.algorithm,
            algorithmState.algorithm,
            "the row's own algorithm is threaded into the memory state, not a default",
        )
    }

    @Test
    fun `null optional columns map to null snapshot fields`() {
        val snapshot = row(state = "New").toSrsCardSnapshot()

        assertNull(snapshot.dueAt)
        assertNull(snapshot.lastReviewedAt)
        assertNull(snapshot.scheduledInterval)
        assertNull(snapshot.stepIndex)
        assertNull(snapshot.parametersId)
        assertNull(snapshot.algorithm)
        assertNull(snapshot.algorithmState)
    }

    @Test
    fun `algorithm info requires both a name and a version`() {
        val snapshot =
            row(algorithmName = "fsrs", algorithmVersion = null)
                .toSrsCardSnapshot()

        assertNull(snapshot.algorithm, "a name without a version is not a usable algorithm")
        assertNull(snapshot.algorithmState, "no algorithm means no algorithm state")
    }

    @Test
    fun `algorithm state is dropped when difficulty or stability is missing`() {
        val snapshot =
            row(
                algorithmName = "fsrs",
                algorithmVersion = "v6",
                fsrsDifficulty = 5.0,
                fsrsStability = null,
            ).toSrsCardSnapshot()

        assertEquals("fsrs", snapshot.algorithm?.name, "the algorithm itself is still known")
        assertNull(snapshot.algorithmState, "a half-written memory state is not reconstructed")
    }

    @Test
    fun `state strings round-trip through the enum`() {
        assertEquals(SrsCardState.Relearning, row(state = "Relearning").toSrsCardSnapshot().state)
    }

    private fun row(
        state: String = "New",
        dueAtEpochMs: Long? = null,
        lastReviewedAtEpochMs: Long? = null,
        scheduledIntervalMs: Long? = null,
        reviewCount: Long = 0L,
        lapseCount: Long = 0L,
        stepIndex: Long? = null,
        algorithmName: String? = null,
        algorithmVersion: String? = null,
        fsrsDifficulty: Double? = null,
        fsrsStability: Double? = null,
        parametersId: String? = null,
    ): Practice_srs_card =
        Practice_srs_card(
            card_id = "card-1",
            state = state,
            due_at_epoch_ms = dueAtEpochMs,
            last_reviewed_at_epoch_ms = lastReviewedAtEpochMs,
            scheduled_interval_ms = scheduledIntervalMs,
            review_count = reviewCount,
            lapse_count = lapseCount,
            step_index = stepIndex,
            algorithm_name = algorithmName,
            algorithm_version = algorithmVersion,
            fsrs_difficulty = fsrsDifficulty,
            fsrs_stability = fsrsStability,
            parameters_id = parametersId,
        )
}
