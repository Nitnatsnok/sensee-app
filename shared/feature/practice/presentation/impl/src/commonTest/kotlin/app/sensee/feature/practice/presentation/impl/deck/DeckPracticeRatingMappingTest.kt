package app.sensee.feature.practice.presentation.impl.deck

import app.sensee.feature.practice.presentation.api.DeckPracticeRatingAction
import app.sensee.ui.learningDeck.LearningSwipeDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class DeckPracticeRatingMappingTest {
    @Test
    fun `the swipe compass maps each direction to its symmetric rating`() {
        // Difficulty eases Start -> Down -> Up -> End (Again -> Hard -> Good -> Easy):
        // horizontal extremes carry the rating extremes, vertical the two middles.
        assertEquals(DeckPracticeRatingAction.Again, LearningSwipeDirection.Start.toRating())
        assertEquals(DeckPracticeRatingAction.Hard, LearningSwipeDirection.Down.toRating())
        assertEquals(DeckPracticeRatingAction.Good, LearningSwipeDirection.Up.toRating())
        assertEquals(DeckPracticeRatingAction.Easy, LearningSwipeDirection.End.toRating())
    }

    @Test
    fun `every swipe direction maps to a distinct rating`() {
        val ratings = LearningSwipeDirection.entries.map { it.toRating() }
        assertEquals(ratings.toSet().size, ratings.size, "no two directions share a rating")
        assertEquals(DeckPracticeRatingAction.entries.toSet(), ratings.toSet())
    }
}
