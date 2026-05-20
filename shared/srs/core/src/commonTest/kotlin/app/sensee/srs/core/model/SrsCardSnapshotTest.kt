package app.sensee.srs.core.model

import app.sensee.srs.core.id.SrsCardId
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SrsCardSnapshotTest {
    @Test
    fun `throws for negative review count`() {
        assertFailsWith<IllegalArgumentException> {
            SrsCardSnapshot(
                id = SrsCardId("card-1"),
                state = SrsCardState.New,
                dueAt = null,
                lastReviewedAt = null,
                scheduledInterval = null,
                reviewCount = -1,
                lapseCount = 0,
                stepIndex = null,
                algorithmState = null,
                algorithm = null,
                parametersId = null,
            )
        }
    }

    @Test
    fun `throws for negative lapse count`() {
        assertFailsWith<IllegalArgumentException> {
            SrsCardSnapshot(
                id = SrsCardId("card-1"),
                state = SrsCardState.New,
                dueAt = null,
                lastReviewedAt = null,
                scheduledInterval = null,
                reviewCount = 0,
                lapseCount = -1,
                stepIndex = null,
                algorithmState = null,
                algorithm = null,
                parametersId = null,
            )
        }
    }
}
