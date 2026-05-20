package app.sensee.srs.core.algorithm

import app.sensee.srs.core.model.ReviewRating
import app.sensee.srs.core.model.SrsCardSnapshot
import kotlin.time.Instant

public data class SrsSchedulingPreview(
    val sourceCard: SrsCardSnapshot,
    val reviewedAt: Instant,
    val ratings: List<SrsRatingPreview>,
) {
    public fun get(rating: ReviewRating): SrsRatingPreview = ratings.first { it.rating == rating }
}
