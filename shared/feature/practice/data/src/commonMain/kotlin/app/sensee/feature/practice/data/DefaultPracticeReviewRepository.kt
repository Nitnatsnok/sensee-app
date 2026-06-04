package app.sensee.feature.practice.data

import app.sensee.feature.practice.domain.CardReview
import app.sensee.feature.practice.domain.PracticeReviewRepository
import app.sensee.feature.practice.domain.ReviewOutcome
import app.sensee.srs.engine.SrsEngine
import app.sensee.srs.engine.SrsReviewRequest
import app.sensee.srs.fsrs.FsrsParameters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

/**
 * Practice-owned write path: advance the card's SRS state through the engine and
 * return the refreshed snapshot. The catalog projection stays in Library, so this
 * no longer re-reads a `Card` — `practice` does not depend on `library`.
 */
@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<PracticeReviewRepository>(),
)
@Inject
public class DefaultPracticeReviewRepository(
    private val srsEngine: SrsEngine<FsrsParameters>,
) : PracticeReviewRepository {
    override suspend fun submitReview(review: CardReview): ReviewOutcome {
        val result =
            srsEngine.submitReview(
                SrsReviewRequest(
                    cardId = review.cardId,
                    rating = review.rating,
                ),
            )
        return ReviewOutcome(srs = result.updatedCard)
    }
}
