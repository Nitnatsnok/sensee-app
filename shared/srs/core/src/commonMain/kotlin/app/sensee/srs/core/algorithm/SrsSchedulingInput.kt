package app.sensee.srs.core.algorithm

import app.sensee.srs.core.id.SrsReviewLogId
import app.sensee.srs.core.model.ReviewRating
import app.sensee.srs.core.model.SrsAlgorithmParameters
import app.sensee.srs.core.model.SrsCardSnapshot
import kotlin.time.Instant

public data class SrsSchedulingInput<Parameters : SrsAlgorithmParameters>(
    val card: SrsCardSnapshot,
    val rating: ReviewRating,
    val reviewedAt: Instant,
    val parameters: Parameters,
    val reviewLogId: SrsReviewLogId,
)
