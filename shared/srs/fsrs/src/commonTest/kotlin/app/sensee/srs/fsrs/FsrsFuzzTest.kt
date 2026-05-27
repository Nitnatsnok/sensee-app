package app.sensee.srs.fsrs

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FsrsFuzzTest {
    @Test
    fun `intervals below the threshold are returned unchanged`() {
        val random = Random(seed = 1L)
        for (interval in 0..2) {
            assertEquals(
                expected = interval,
                actual =
                    fuzzedIntervalDays(
                        intervalDays = interval,
                        maximumIntervalDays = MAX_INTERVAL,
                        random = random,
                    ),
            )
        }
    }

    @Test
    fun `interval of three day boundary is fuzzed within the expected window`() {
        // py-fsrs: delta = 1 + 0.15 * 0.5 = 1.075 ⇒ rawMin=2, rawMax=4.
        // `randint(rawMin, rawMax)` inclusive ⇒ {2, 3, 4}.
        val random = Random(seed = 1L)
        repeat(REPEAT_COUNT) {
            val out =
                fuzzedIntervalDays(
                    intervalDays = 3,
                    maximumIntervalDays = MAX_INTERVAL,
                    random = random,
                )
            assertTrue(out in 2..4, "expected out in [2, 4] but was $out")
        }
    }

    @Test
    fun `interval of ten falls within the piecewise computed bounds`() {
        // py-fsrs: delta = 1 + 0.15*4.5 + 0.1*3 = 1.975 ⇒ rawMin=8, rawMax=12.
        // `randint(rawMin, rawMax)` inclusive ⇒ {8..12}.
        val random = Random(seed = 7L)
        repeat(REPEAT_COUNT) {
            val out =
                fuzzedIntervalDays(
                    intervalDays = 10,
                    maximumIntervalDays = MAX_INTERVAL,
                    random = random,
                )
            assertTrue(out in 8..12, "expected out in [8, 12] but was $out")
        }
    }

    @Test
    fun `fuzzed interval never exceeds the configured maximum`() {
        // maximumIntervalDays caps the upper bound of the fuzz window — verify by setting
        // the cap right at the input interval.
        val random = Random(seed = 1L)
        val maximum = 10
        repeat(REPEAT_COUNT) {
            val out =
                fuzzedIntervalDays(
                    intervalDays = maximum,
                    maximumIntervalDays = maximum,
                    random = random,
                )
            assertTrue(out <= maximum, "expected out <= $maximum but was $out")
            assertTrue(out >= 2, "expected out >= 2 (fuzz floor) but was $out")
        }
    }

    private companion object {
        const val MAX_INTERVAL: Int = 36_500
        const val REPEAT_COUNT: Int = 200
    }
}
