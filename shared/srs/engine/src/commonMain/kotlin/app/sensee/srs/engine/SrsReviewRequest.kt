package app.sensee.srs.engine

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.id.SrsScope
import app.sensee.srs.core.model.ReviewRating
import kotlin.time.Instant

public data class SrsReviewRequest(
    val cardId: SrsCardId,
    val rating: ReviewRating,
    val reviewedAt: Instant? = null,
    val scope: SrsScope = SrsScope.Default,
)
