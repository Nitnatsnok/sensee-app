package app.sensee.srs.fsrs

import app.sensee.srs.core.model.ReviewRating
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FsrsMathTest {
    private val math =
        FsrsMath(
            parameters = FsrsParameters.defaultV6(),
        )

    @Test
    fun `retrievability equals about desired retention when elapsed days equals stability`() {
        val retrievability =
            math.retrievability(
                elapsedDays = 10.0,
                stability = 10.0,
            )

        assertEquals(
            expected = 0.9,
            actual = retrievability,
            absoluteTolerance = 0.0001,
        )
    }

    @Test
    fun `initial state has positive stability and bounded difficulty`() {
        val state = math.initialState(ReviewRating.Good)

        assertTrue(state.stability > 0.0)
        assertTrue(state.difficulty in 1.0..10.0)
    }

    @Test
    fun `next interval is positive`() {
        val interval = math.nextIntervalDays(stability = 5.0)

        assertTrue(interval > 0)
    }

    @Test
    fun `easy initial difficulty is lower than again initial difficulty`() {
        val again = math.initialState(ReviewRating.Again)
        val easy = math.initialState(ReviewRating.Easy)

        assertTrue(easy.difficulty < again.difficulty)
    }

    @Test
    fun `successful review does not reduce short term stability`() {
        val next =
            math.nextShortTermStability(
                currentStability = 1.0,
                rating = ReviewRating.Good,
            )

        assertTrue(next >= 1.0)
    }
}
