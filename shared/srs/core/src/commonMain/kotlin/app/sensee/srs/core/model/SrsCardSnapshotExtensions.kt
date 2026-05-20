package app.sensee.srs.core.model

import app.sensee.srs.core.log.SrsCardReviewSnapshot

public fun SrsCardSnapshot.toReviewSnapshot(): SrsCardReviewSnapshot =
    SrsCardReviewSnapshot(
        state = state,
        dueAt = dueAt,
        scheduledInterval = scheduledInterval,
        reviewCount = reviewCount,
        lapseCount = lapseCount,
        stepIndex = stepIndex,
        algorithmState = algorithmState,
    )
