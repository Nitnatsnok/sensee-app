package app.sensee.feature.practice.data

import app.sensee.feature.library.domain.Card
import app.sensee.feature.library.domain.CatalogRepository
import app.sensee.feature.practice.domain.CardReview
import app.sensee.feature.practice.domain.PracticeReviewRepository
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.engine.SrsEngine
import app.sensee.srs.engine.SrsReviewRequest
import app.sensee.srs.fsrs.FsrsParameters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

/**
 * Practice-owned write path: advance the card's SRS state through the engine, then return the
 * refreshed card by re-reading it through the catalog contract. Practice depends only on the
 * [CatalogRepository] contract, never on the catalog data implementation.
 */
@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<PracticeReviewRepository>(),
)
@Inject
public class DefaultPracticeReviewRepository(
    private val srsEngine: SrsEngine<FsrsParameters>,
    private val catalogRepository: CatalogRepository,
) : PracticeReviewRepository {
    override suspend fun submitReview(review: CardReview): Card {
        srsEngine.submitReview(
            SrsReviewRequest(
                cardId = SrsCardId(review.cardId.value),
                rating = review.rating,
            ),
        )
        return catalogRepository.loadCard(review.cardId)
    }
}
