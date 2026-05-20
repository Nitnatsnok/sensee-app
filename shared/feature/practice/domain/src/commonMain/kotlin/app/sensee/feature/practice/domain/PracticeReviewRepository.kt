package app.sensee.feature.practice.domain

import app.sensee.feature.library.domain.Card

/**
 * Practice-owned mutation of learning progress. Submitting a review advances the card's SRS
 * state and returns the refreshed [Card]. Catalog reads live behind Library's
 * `CatalogRepository`, not here.
 */
public interface PracticeReviewRepository {
    public suspend fun submitReview(review: CardReview): Card
}
