package app.sensee.srs.core.algorithm

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.model.ReviewRating
import app.sensee.srs.core.model.SrsCardSnapshot
import app.sensee.srs.core.model.SrsCardState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class SrsSchedulingPreviewExtensionsTest {
    @Test
    fun `converts scheduling preview to compact review options preview`() {
        val reviewedAt = Instant.parse("2026-04-29T10:00:00Z")
        val sourceCard = newCard()

        val preview =
            SrsSchedulingPreview(
                sourceCard = sourceCard,
                reviewedAt = reviewedAt,
                ratings =
                    listOf(
                        SrsRatingPreview(
                            rating = ReviewRating.Again,
                            updatedCard =
                                sourceCard.copy(
                                    state = SrsCardState.Learning,
                                    dueAt = reviewedAt + 10.minutes,
                                    scheduledInterval = 10.minutes,
                                    stepIndex = 0,
                                ),
                        ),
                        SrsRatingPreview(
                            rating = ReviewRating.Good,
                            updatedCard =
                                sourceCard.copy(
                                    state = SrsCardState.Review,
                                    dueAt = reviewedAt + 1.days,
                                    scheduledInterval = 1.days,
                                    stepIndex = null,
                                ),
                        ),
                    ),
            )

        val options = preview.toReviewOptionsPreview()

        assertEquals(sourceCard.id, options.cardId)
        assertEquals(reviewedAt, options.reviewedAt)

        val again = options.get(ReviewRating.Again)
        val good = options.get(ReviewRating.Good)

        assertEquals(SrsCardState.Learning, again.state)
        assertEquals(10.minutes, again.scheduledInterval)
        assertEquals(0, again.stepIndex)

        assertEquals(SrsCardState.Review, good.state)
        assertEquals(1.days, good.scheduledInterval)
        assertEquals(null, good.stepIndex)
    }

    private fun newCard(): SrsCardSnapshot =
        SrsCardSnapshot(
            id = SrsCardId("card-1"),
            state = SrsCardState.New,
            dueAt = null,
            lastReviewedAt = null,
            scheduledInterval = null,
            reviewCount = 0,
            lapseCount = 0,
            stepIndex = null,
            algorithmState = null,
            algorithm = null,
            parametersId = null,
        )
}
