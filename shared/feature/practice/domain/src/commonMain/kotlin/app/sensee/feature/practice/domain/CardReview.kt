package app.sensee.feature.practice.domain

import app.sensee.feature.library.domain.CardId
import app.sensee.srs.core.model.ReviewRating

public data class CardReview(
    val cardId: CardId,
    val rating: ReviewRating,
)
