package app.sensee.srs.fsrs

import app.sensee.srs.core.model.ReviewRating
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

internal class FsrsMath(
    private val parameters: FsrsParameters,
) {
    private val w: List<Double> = parameters.weights

    /**
     * FSRS v6:
     * R(t, S) = (1 + factor * t / S) ^ decay
     *
     * decay = -w[20]
     * factor is chosen so that R(S, S) = 0.9
     */
    private val decay: Double = -w[20]

    private val factor: Double =
        0.9.pow(1.0 / decay) - 1.0

    fun initialState(rating: ReviewRating): FsrsAlgorithmState =
        FsrsAlgorithmState(
            difficulty = initialDifficulty(rating),
            stability = initialStability(rating),
        )

    fun retrievability(
        elapsedDays: Double,
        stability: Double,
    ): Double {
        require(elapsedDays >= 0.0) {
            "elapsedDays must be non-negative"
        }

        require(stability > 0.0) {
            "stability must be positive"
        }

        return (1.0 + factor * elapsedDays / stability)
            .pow(decay)
            .coerceIn(0.0, 1.0)
    }

    fun nextIntervalDays(stability: Double): Int {
        require(stability > 0.0) {
            "stability must be positive"
        }

        val rawInterval =
            stability / factor *
                (parameters.desiredRetention.pow(1.0 / decay) - 1.0)

        return rawInterval
            .roundToInt()
            .coerceIn(1, parameters.maximumIntervalDays)
    }

    /**
     * FSRS v6 difficulty update:
     * D' = clamp(meanReversion(D + linearDamping(-w[6] * (g - 3), D)), 1.0, 10.0)
     *
     * w[6] scales the per-grade delta (g = fsrsGrade ∈ {1..4}; g = 3 ≡ Good ≡ no change).
     * `linearDamping` shrinks the delta as D approaches 10; `meanReversion` (weighted by w[7])
     * pulls toward the Easy-initial difficulty over the long run.
     */
    fun nextDifficulty(
        currentDifficulty: Double,
        rating: ReviewRating,
    ): Double {
        val grade = rating.fsrsGrade

        val deltaDifficulty = -w[6] * (grade - 3)
        val dampedDelta =
            linearDamping(
                delta = deltaDifficulty,
                oldDifficulty = currentDifficulty,
            )

        val nextDifficulty = currentDifficulty + dampedDelta

        return meanReversion(
            initDifficulty = initialDifficulty(ReviewRating.Easy),
            nextDifficulty = nextDifficulty,
        ).coerceIn(1.0, 10.0)
    }

    /**
     * FSRS v6 short-term stability update (used inside the learning step / same-day window):
     * S' = S * max(SI, 1.0 if g ≥ 3 else SI)
     * SI = exp(w[17] * (g - 3 + w[18])) * S^(-w[19])
     *
     * The `coerceAtLeast(1.0)` floor only applies for non-Again ratings — Again is allowed to
     * shrink stability even on a same-day repeat.
     */
    fun nextShortTermStability(
        currentStability: Double,
        rating: ReviewRating,
    ): Double {
        val grade = rating.fsrsGrade

        var stabilityIncrease =
            exp(w[17] * (grade - 3 + w[18])) *
                currentStability.pow(-w[19])

        if (grade >= 3) {
            stabilityIncrease = stabilityIncrease.coerceAtLeast(1.0)
        }

        return currentStability * stabilityIncrease
    }

    /**
     * FSRS v6 stability update on successful recall (Hard / Good / Easy):
     * S' = S * (1 + SI)
     * SI = exp(w[8]) * (11 - D) * S^(-w[9]) * (exp((1 - R) * w[10]) - 1) * hardPenalty * easyBonus
     *
     * hardPenalty = w[15] for Hard, 1.0 otherwise; easyBonus = w[16] for Easy, 1.0 otherwise.
     * The `(1 - R) * w[10]` term means low retrievability (a "useful" successful recall) earns
     * more stability than recall when the item was still well retained.
     */
    fun nextRecallStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        rating: ReviewRating,
    ): Double {
        val hardPenalty =
            if (rating == ReviewRating.Hard) {
                w[15]
            } else {
                1.0
            }

        val easyBonus =
            if (rating == ReviewRating.Easy) {
                w[16]
            } else {
                1.0
            }

        val stabilityIncrease =
            exp(w[8]) *
                (11.0 - difficulty) *
                stability.pow(-w[9]) *
                (exp((1.0 - retrievability) * w[10]) - 1.0) *
                hardPenalty *
                easyBonus

        return stability * (1.0 + stabilityIncrease)
    }

    /**
     * FSRS v6 stability update on a failed recall (Again):
     * S' = clamp(min(S_forget, S_min), 0.01, ∞)
     * S_forget  = w[11] * D^(-w[12]) * ((S + 1)^w[13] - 1) * exp((1 - R) * w[14])
     * S_min     = S / exp(w[17] * w[18])      // floor: lapse cannot drop S below same-day baseline
     *
     * The `min(..., S_min)` cap prevents lapse stability from exceeding the same-day floor
     * derived from the short-term stability formula; the `coerceAtLeast(0.01)` keeps S strictly
     * positive so subsequent `retrievability()` does not divide by zero.
     */
    fun nextForgetStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
    ): Double {
        val minimumStability =
            stability / exp(w[17] * w[18])

        val nextStability =
            w[11] *
                difficulty.pow(-w[12]) *
                ((stability + 1.0).pow(w[13]) - 1.0) *
                exp((1.0 - retrievability) * w[14])

        return min(nextStability, minimumStability)
            .coerceAtLeast(0.01)
    }

    /**
     * Initial stability after a brand-new card's first answer.
     * w[0..3] hold the per-rating starting stabilities (index = fsrsGrade - 1, so
     * Again=w[0], Hard=w[1], Good=w[2], Easy=w[3]). Floored at 0.001 to keep
     * downstream division by stability finite.
     */
    private fun initialStability(rating: ReviewRating): Double =
        w[rating.fsrsGrade - 1]
            .coerceAtLeast(0.001)

    /**
     * Initial difficulty after a brand-new card's first answer:
     * D₀ = clamp(w[4] - exp(w[5] * (g - 1)) + 1, 1.0, 10.0)
     *
     * w[4] sets the Again baseline (g = 1 ⇒ exp(0) = 1, so D₀ = w[4]); w[5] controls how
     * sharply better first ratings reduce starting difficulty.
     */
    private fun initialDifficulty(rating: ReviewRating): Double {
        val grade = rating.fsrsGrade

        return (w[4] - exp(w[5] * (grade - 1)) + 1.0)
            .coerceIn(1.0, 10.0)
    }

    private fun linearDamping(
        delta: Double,
        oldDifficulty: Double,
    ): Double = (10.0 - oldDifficulty) * delta / 9.0

    private fun meanReversion(
        initDifficulty: Double,
        nextDifficulty: Double,
    ): Double = w[7] * initDifficulty + (1.0 - w[7]) * nextDifficulty
}
