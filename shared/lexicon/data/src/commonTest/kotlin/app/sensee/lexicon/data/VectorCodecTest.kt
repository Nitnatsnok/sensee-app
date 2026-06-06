package app.sensee.lexicon.data

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The float32 vector codec backing the `sense_embedding` BLOB: a vector survives
 * the byte round-trip bit-for-bit on every target, the blob is four bytes per
 * float, normalization yields unit length, an all-zero vector cannot be
 * normalized, and a ragged blob is rejected.
 */
class VectorCodecTest {
    @Test
    fun `a vector round-trips through its byte form`() {
        val vector = floatArrayOf(1.5f, -2.25f, 0f, 3.0e10f, Float.MIN_VALUE, Float.MAX_VALUE)

        assertTrue(vector.contentEquals(vector.toVectorBytes().toFloatVector()))
    }

    @Test
    fun `the blob is four bytes per float`() {
        assertEquals(12, floatArrayOf(1f, 2f, 3f).toVectorBytes().size)
    }

    @Test
    fun `normalizing yields unit length`() {
        // 3-4-5 triangle → (0.6, 0.8), length 1.
        val normalized = floatArrayOf(3f, 4f).l2Normalized()!!

        val length = sqrt(normalized[0] * normalized[0] + normalized[1] * normalized[1])
        assertTrue(abs(1f - length) < 1e-6f, "expected unit length, was $length")
    }

    @Test
    fun `an all-zero vector cannot be normalized`() {
        assertNull(floatArrayOf(0f, 0f, 0f).l2Normalized())
    }

    @Test
    fun `a blob whose length is not a whole number of floats is rejected`() {
        assertFailsWith<IllegalArgumentException> { byteArrayOf(1, 2, 3).toFloatVector() }
    }
}
