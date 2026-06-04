package app.sensee.feature.practice.domain

/**
 * Practice-owned mutation of learning progress. Submitting a review advances the
 * card's SRS state and returns a practice-owned [ReviewOutcome]; the catalog
 * projection stays behind Library's `CatalogRepository`, so this contract never
 * returns a `Card` and `practice/domain` does not depend on `library`.
 */
public interface PracticeReviewRepository {
    public suspend fun submitReview(review: CardReview): ReviewOutcome
}
