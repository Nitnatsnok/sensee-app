package app.sensee.srs.core.algorithm

import app.sensee.srs.core.model.ReviewRating
import app.sensee.srs.core.model.SrsCardSnapshot

public data class SrsRatingPreview(
    val rating: ReviewRating,
    val updatedCard: SrsCardSnapshot,
)
