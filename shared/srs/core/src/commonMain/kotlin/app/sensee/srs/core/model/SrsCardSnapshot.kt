package app.sensee.srs.core.model

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.id.SrsParametersId
import kotlin.time.Duration
import kotlin.time.Instant

public data class SrsCardSnapshot(
    val id: SrsCardId,
    val state: SrsCardState,
    val dueAt: Instant?,
    val lastReviewedAt: Instant?,
    val scheduledInterval: Duration?,
    val reviewCount: Int,
    val lapseCount: Int,
    val stepIndex: Int?,
    val algorithmState: SrsAlgorithmState?,
    val algorithm: SrsAlgorithmInfo?,
    val parametersId: SrsParametersId?,
) {
    init {
        require(reviewCount >= 0) {
            "reviewCount must be non-negative"
        }
        require(lapseCount >= 0) {
            "lapseCount must be non-negative"
        }
        require(stepIndex == null || stepIndex >= 0) {
            "stepIndex must be null or non-negative"
        }
    }
}
