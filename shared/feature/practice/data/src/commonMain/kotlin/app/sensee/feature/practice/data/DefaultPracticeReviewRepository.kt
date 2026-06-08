package app.sensee.feature.practice.data

import app.sensee.feature.practice.domain.CardReview
import app.sensee.feature.practice.domain.PracticeReviewRepository
import app.sensee.feature.practice.domain.ReviewOutcome
import app.sensee.srs.engine.SrsEngine
import app.sensee.srs.engine.SrsReviewRequest
import app.sensee.srs.engine.factory.SrsCardFactory
import app.sensee.srs.engine.storage.SrsStorage
import app.sensee.srs.fsrs.FsrsParameters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

/**
 * Practice-owned write path: advance the card's SRS state through the engine and
 * return the refreshed snapshot. The catalog projection stays in Library, so this
 * creates the SRS seat explicitly before review instead of relying on Library
 * reads to materialize it.
 */
@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<PracticeReviewRepository>(),
)
@Inject
public class DefaultPracticeReviewRepository(
    private val srsStorage: SrsStorage<FsrsParameters>,
    private val srsEngine: SrsEngine<FsrsParameters>,
) : PracticeReviewRepository {
    override suspend fun submitReview(review: CardReview): ReviewOutcome {
        srsStorage.saveCardIfAbsent(SrsCardFactory.newCard(review.cardId))
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
