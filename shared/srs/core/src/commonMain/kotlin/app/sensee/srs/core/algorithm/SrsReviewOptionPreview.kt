package app.sensee.srs.core.algorithm

import app.sensee.srs.core.model.ReviewRating
import app.sensee.srs.core.model.SrsCardState
import kotlin.time.Duration
import kotlin.time.Instant

public data class SrsReviewOptionPreview(
    val rating: ReviewRating,
    val state: SrsCardState,
    val dueAt: Instant?,
    val scheduledInterval: Duration?,
    val stepIndex: Int?,
)
