package app.sensee.srs.fsrs

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * FSRS interval fuzzing: a small uniformly-distributed jitter applied to the calculated
 * Review-state interval so that batches of cards reviewed on the same day do not all come
 * due together. Matches py-fsrs `_get_fuzzed_interval`.
 *
 * Intervals strictly below [FUZZ_INTERVAL_THRESHOLD_DAYS] are returned unchanged.
 * For longer intervals the allowed jitter half-width `delta` grows piecewise as a
 * function of the interval length (see [FUZZ_RANGES]); the result is sampled uniformly
 * from `[max(2, round(I - delta)) .. min(maximumIntervalDays, round(I + delta))]` and
 * finally capped at [maximumIntervalDays].
 */
internal fun fuzzedIntervalDays(
    intervalDays: Int,
    maximumIntervalDays: Int,
    random: Random,
): Int {
    if (intervalDays < FUZZ_INTERVAL_THRESHOLD_DAYS) return intervalDays

    val delta = fuzzDelta(intervalDays.toDouble())

    val rawMin = (intervalDays - delta).roundToInt()
    val rawMax = (intervalDays + delta).roundToInt()

    val maxIvl = minOf(rawMax, maximumIntervalDays)
    val minIvl = maxOf(MIN_FUZZ_INTERVAL_DAYS, rawMin).coerceAtMost(maxIvl)

    val span = maxIvl - minIvl + 1
    return (minIvl + random.nextInt(span)).coerceAtMost(maximumIntervalDays)
}

private fun fuzzDelta(intervalDays: Double): Double {
    var delta = FUZZ_BASE_DELTA
    for (range in FUZZ_RANGES) {
        delta += range.factor * maxOf(minOf(intervalDays, range.end) - range.start, 0.0)
    }
    return delta
}

private data class FuzzRange(
    val start: Double,
    val end: Double,
    val factor: Double,
)

/**
 * Piecewise-linear delta coefficients (py-fsrs `FUZZ_RANGES`): wider ranges contribute
 * proportionally less jitter, so the fuzz fraction shrinks as intervals grow.
 */
private val FUZZ_RANGES: List<FuzzRange> =
    listOf(
        FuzzRange(start = 2.5, end = 7.0, factor = 0.15),
        FuzzRange(start = 7.0, end = 20.0, factor = 0.1),
        FuzzRange(start = 20.0, end = Double.POSITIVE_INFINITY, factor = 0.05),
    )

/** Intervals below this many days are returned unchanged (py-fsrs: `interval_days < 2.5`). */
private const val FUZZ_INTERVAL_THRESHOLD_DAYS: Int = 3

/** Floor for the lower fuzz bound: the fuzzed interval is never shorter than this. */
private const val MIN_FUZZ_INTERVAL_DAYS: Int = 2

/** Constant `1.0` added to `delta` independently of interval length (py-fsrs `delta = 1.0` seed). */
private const val FUZZ_BASE_DELTA: Double = 1.0
