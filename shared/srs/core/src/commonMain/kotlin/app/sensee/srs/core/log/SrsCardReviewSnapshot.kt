package app.sensee.srs.core.log

import app.sensee.srs.core.model.SrsAlgorithmState
import app.sensee.srs.core.model.SrsCardState
import kotlin.time.Duration
import kotlin.time.Instant

public data class SrsCardReviewSnapshot(
    val state: SrsCardState,
    val dueAt: Instant?,
    val scheduledInterval: Duration?,
    val reviewCount: Int,
    val lapseCount: Int,
    val stepIndex: Int?,
    val algorithmState: SrsAlgorithmState?,
) {
    init {
        require(reviewCount >= 0) {
            "reviewCount must be non-negative"
        }

        require(lapseCount >= 0) {
            "lapseCount must be non-negative"
        }
    }
}
