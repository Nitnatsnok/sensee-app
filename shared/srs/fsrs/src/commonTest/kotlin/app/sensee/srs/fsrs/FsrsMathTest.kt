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
    fun `initial state matches the FSRS v6 default weights`() {
        val state = math.initialState(ReviewRating.Good)

        assertEquals(
            expected = 2.11810397045901,
            actual = state.difficulty,
            absoluteTolerance = 1e-12,
        )
        assertEquals(
            expected = 2.3065,
            actual = state.stability,
            absoluteTolerance = 1e-12,
        )
    }

    @Test
    fun `next interval rounds stability at the desired retention point`() {
        val interval = math.nextIntervalDays(stability = 5.0)

        assertEquals(5, interval)
    }

    @Test
    fun `easy initial difficulty is lower than again initial difficulty`() {
        val again = math.initialState(ReviewRating.Again)
        val easy = math.initialState(ReviewRating.Easy)

        assertTrue(easy.difficulty < again.difficulty)
    }

    @Test
    fun `successful short term review uses the FSRS v6 stability increase`() {
        val next =
            math.nextShortTermStability(
                currentStability = 1.0,
                rating = ReviewRating.Good,
            )

        assertEquals(
            expected = 1.05072037462322,
            actual = next,
            absoluteTolerance = 1e-12,
        )
    }

    @Test
    fun `mean reversion target is unclamped D zero of easy`() {
        // Reference (py-fsrs `clamp=False`, fsrs-rs `init_difficulty(4)`): raw D₀(Easy) =
        // w[4] - exp(w[5]·3) + 1 ≈ -4.7716 is the mean-reversion target. With currentD=5
        // and Good (zero delta) ⇒ mean_rev = 0.001·(-4.7716) + 0.999·5 ≈ 4.9902.
        // Clamping the target to 1.0 would give 4.996 — distinguishable at tolerance 1e-4.
        val next =
            math.nextDifficulty(
                currentDifficulty = 5.0,
                rating = ReviewRating.Good,
            )

        assertEquals(
            expected = 4.9902,
            actual = next,
            absoluteTolerance = 0.0001,
        )
    }

    @Test
    fun `forget stability floors at the official 0_001 minimum`() {
        // S_forget here is ≈ 3.4e-4, below STABILITY_MIN = 0.001 — floor activates and the
        // result must equal 0.001 (py-fsrs/fsrs-rs `clamp_stability(STABILITY_MIN)`).
        val next =
            math.nextForgetStability(
                difficulty = 10.0,
                stability = 0.001,
                retrievability = 1.0,
            )

        assertEquals(
            expected = 0.001,
            actual = next,
            absoluteTolerance = 1e-9,
        )
    }

    @Test
    fun `short term stability does not drop below the official minimum`() {
        // Tiny S + Again grade produces S * SI ≈ 6.5e-5 — must be floored at STABILITY_MIN.
        val next =
            math.nextShortTermStability(
                currentStability = 0.0001,
                rating = ReviewRating.Again,
            )

        assertTrue(
            actual = next >= 0.001,
            message = "expected next >= 0.001 but was $next",
        )
    }
}
