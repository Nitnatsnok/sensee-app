package app.sensee.srs.core.algorithm

public fun SrsSchedulingPreview.toReviewOptionsPreview(): SrsReviewOptionsPreview =
    SrsReviewOptionsPreview(
        cardId = sourceCard.id,
        reviewedAt = reviewedAt,
        options =
            ratings.map { ratingPreview ->
                val updatedCard = ratingPreview.updatedCard

                SrsReviewOptionPreview(
                    rating = ratingPreview.rating,
                    state = updatedCard.state,
                    dueAt = updatedCard.dueAt,
                    scheduledInterval = updatedCard.scheduledInterval,
                    stepIndex = updatedCard.stepIndex,
                )
            },
    )
