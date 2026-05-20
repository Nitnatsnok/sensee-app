package app.sensee.srs.core.algorithm

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.model.ReviewRating
import kotlin.time.Instant

public data class SrsReviewOptionsPreview(
    val cardId: SrsCardId,
    val reviewedAt: Instant,
    val options: List<SrsReviewOptionPreview>,
) {
    public fun get(rating: ReviewRating): SrsReviewOptionPreview = options.first { it.rating == rating }
}
