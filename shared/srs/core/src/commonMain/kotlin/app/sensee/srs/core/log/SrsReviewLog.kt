package app.sensee.srs.core.log

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.id.SrsParametersId
import app.sensee.srs.core.id.SrsReviewLogId
import app.sensee.srs.core.model.ReviewRating
import app.sensee.srs.core.model.SrsAlgorithmInfo
import kotlin.time.Instant

public data class SrsReviewLog(
    val id: SrsReviewLogId,
    val cardId: SrsCardId,
    val rating: ReviewRating,
    val reviewedAt: Instant,
    val previous: SrsCardReviewSnapshot,
    val next: SrsCardReviewSnapshot,
    val algorithm: SrsAlgorithmInfo,
    val parametersId: SrsParametersId,
)
