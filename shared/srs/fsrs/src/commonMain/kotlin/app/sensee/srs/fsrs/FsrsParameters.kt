package app.sensee.srs.fsrs

import app.sensee.srs.core.id.SrsParametersId
import app.sensee.srs.core.model.SrsAlgorithmInfo
import app.sensee.srs.core.model.SrsAlgorithmParameters
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

public data class FsrsParameters(
    override val id: SrsParametersId,
    override val algorithm: SrsAlgorithmInfo = FsrsAlgorithm.V6,
    val weights: List<Double>,
    val desiredRetention: Double = 0.9,
    val learningSteps: List<Duration> =
        listOf(
            1.minutes,
            10.minutes,
        ),
    val relearningSteps: List<Duration> =
        listOf(
            10.minutes,
        ),
    val maximumIntervalDays: Int = 36_500,
    /**
     * Applies a small uniform jitter to Review-state intervals so cards reviewed on the
     * same day do not all come due together (py-fsrs `_get_fuzzed_interval`). Applied
     * only by `FsrsScheduler.schedule()`; `preview()` always returns deterministic
     * intervals.
     */
    val enableFuzzing: Boolean = true,
) : SrsAlgorithmParameters {
    init {
        require(weights.size == 21) {
            "FSRS v6 requires exactly 21 weights, but ${weights.size} were provided"
        }

        require(desiredRetention > 0.0 && desiredRetention < 1.0) {
            "desiredRetention must be between 0 and 1, exclusive"
        }

        require(maximumIntervalDays > 0) {
            "maximumIntervalDays must be positive"
        }

        require(learningSteps.all { it.isPositive() }) {
            "All learning steps must be positive"
        }

        require(relearningSteps.all { it.isPositive() }) {
            "All relearning steps must be positive"
        }
    }

    public companion object {
        public fun defaultV6(id: SrsParametersId = SrsParametersId("fsrs-v6-default")): FsrsParameters =
            FsrsParameters(
                id = id,
                weights =
                    listOf(
                        0.212,
                        1.2931,
                        2.3065,
                        8.2956,
                        6.4133,
                        0.8334,
                        3.0194,
                        0.001,
                        1.8722,
                        0.1666,
                        0.796,
                        1.4835,
                        0.0614,
                        0.2629,
                        1.6483,
                        0.6014,
                        1.8729,
                        0.5425,
                        0.0912,
                        0.0658,
                        0.1542,
                    ),
            )
    }
}
