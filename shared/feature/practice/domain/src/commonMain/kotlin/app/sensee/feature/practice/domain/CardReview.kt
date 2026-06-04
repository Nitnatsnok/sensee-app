package app.sensee.feature.practice.domain

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.model.ReviewRating

/**
 * A review to submit: which card ([SrsCardId], the SRS key) and how it was rated.
 * Keyed by the SRS id rather than a library `CardId` so the practice contract
 * stays free of the catalog projection.
 */
public data class CardReview(
    val cardId: SrsCardId,
    val rating: ReviewRating,
)
