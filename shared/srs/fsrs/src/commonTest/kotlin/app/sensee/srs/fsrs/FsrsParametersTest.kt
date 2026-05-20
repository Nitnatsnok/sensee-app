package app.sensee.srs.fsrs

import app.sensee.srs.core.id.SrsParametersId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FsrsParametersTest {
    @Test
    fun `creates default v6 parameters`() {
        val parameters = FsrsParameters.defaultV6()

        assertEquals(21, parameters.weights.size)
        assertEquals(0.9, parameters.desiredRetention)
        assertEquals(36_500, parameters.maximumIntervalDays)
    }

    @Test
    fun `throws when weights count is invalid`() {
        assertFailsWith<IllegalArgumentException> {
            FsrsParameters(
                id = SrsParametersId("invalid"),
                weights = listOf(1.0, 2.0),
            )
        }
    }

    @Test
    fun `throws when desired retention is invalid`() {
        assertFailsWith<IllegalArgumentException> {
            FsrsParameters(
                id = SrsParametersId("invalid"),
                weights = List(21) { 1.0 },
                desiredRetention = 1.0,
            )
        }
    }
}
