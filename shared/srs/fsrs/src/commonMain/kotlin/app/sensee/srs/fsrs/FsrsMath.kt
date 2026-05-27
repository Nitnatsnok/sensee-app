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
            difficulty = initialDifficultyRaw(rating).coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY),
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
     * The mean-reversion target is the **unclamped** D₀(Easy), matching py-fsrs
     * (`_initial_difficulty(rating=Easy, clamp=False)`) and fsrs-rs (`init_difficulty(4)`).
     * Only the final result is clamped to `[1, 10]`. Clamping the target early biases the
     * update upward whenever D₀(Easy) raw lies outside `[1, 10]` (with default weights it is
     * negative), and the bias scales with `w[7]`.
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
            initDifficulty = initialDifficultyRaw(ReviewRating.Easy),
            nextDifficulty = nextDifficulty,
        ).coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
    }

    /**
     * FSRS v6 short-term stability update (used inside the learning step / same-day window):
     * S' = max(S * SI, STABILITY_MIN), where SI floors at 1.0 only for Good/Easy.
     * SI = exp(w[17] * (g - 3 + w[18])) * S^(-w[19])
     *
     * The `max(SI, 1.0)` floor only applies for Good/Easy — Again and Hard are allowed to
     * shrink stability even on a same-day repeat (match py-fsrs `rating in (Good, Easy)`).
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

        return (currentStability * stabilityIncrease).coerceAtLeast(STABILITY_MIN)
    }

    /**
     * FSRS v6 stability update on successful recall (Hard / Good / Easy):
     * S' = max(S * (1 + SI), STABILITY_MIN)
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

        return (stability * (1.0 + stabilityIncrease)).coerceAtLeast(STABILITY_MIN)
    }

    /**
     * FSRS v6 stability update on a failed recall (Again):
     * S' = max(min(S_forget, S_min), STABILITY_MIN)
     * S_forget = w[11] * D^(-w[12]) * ((S + 1)^w[13] - 1) * exp((1 - R) * w[14])
     * S_min    = S / exp(w[17] * w[18])      // floor: lapse cannot exceed same-day baseline
     *
     * `min(..., S_min)` caps lapse stability at the same-day floor derived from the short-term
     * formula; the final `coerceAtLeast(STABILITY_MIN)` keeps S above the official 0.001 floor
     * so downstream `retrievability()` does not divide by a near-zero value.
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

        return min(nextStability, minimumStability).coerceAtLeast(STABILITY_MIN)
    }

    /**
     * Initial stability after a brand-new card's first answer.
     * w[0..3] hold the per-rating starting stabilities (index = fsrsGrade - 1, so
     * Again=w[0], Hard=w[1], Good=w[2], Easy=w[3]). Floored at STABILITY_MIN.
     */
    private fun initialStability(rating: ReviewRating): Double = w[rating.fsrsGrade - 1].coerceAtLeast(STABILITY_MIN)

    /**
     * Unclamped initial difficulty:
     * D₀(g) = w[4] - exp(w[5] * (g - 1)) + 1
     *
     * Used **raw** as the mean-reversion target inside [nextDifficulty]. For brand-new cards,
     * [initialState] clamps it to `[MIN_DIFFICULTY, MAX_DIFFICULTY]` (match py-fsrs
     * `_initial_difficulty(clamp=True)`).
     */
    private fun initialDifficultyRaw(rating: ReviewRating): Double {
        val grade = rating.fsrsGrade
        return w[4] - exp(w[5] * (grade - 1)) + 1.0
    }

    private fun linearDamping(
        delta: Double,
        oldDifficulty: Double,
    ): Double = (10.0 - oldDifficulty) * delta / 9.0

    private fun meanReversion(
        initDifficulty: Double,
        nextDifficulty: Double,
    ): Double = w[7] * initDifficulty + (1.0 - w[7]) * nextDifficulty

    private companion object {
        const val STABILITY_MIN: Double = 0.001
        const val MIN_DIFFICULTY: Double = 1.0
        const val MAX_DIFFICULTY: Double = 10.0
    }
}
