package app.sensee.lexicon.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Cosine similarity: identical vectors score 1, orthogonal ones 0, opposite ones
 * -1, an all-zero vector has no direction (0), and mismatched dimensions are
 * rejected. These are the numbers `findSimilar` thresholds against.
 */
class SenseEmbeddingTest {
    private fun assertClose(
        expected: Float,
        actual: Float,
    ) = assertTrue(kotlin.math.abs(expected - actual) < 1e-5f, "expected ~$expected but was $actual")

    @Test
    fun `identical vectors score one`() {
        assertClose(1f, cosineSimilarity(floatArrayOf(1f, 2f, 3f), floatArrayOf(1f, 2f, 3f)))
    }

    @Test
    fun `a scaled vector still scores one`() {
        // Cosine is scale-invariant: direction, not magnitude.
        assertClose(1f, cosineSimilarity(floatArrayOf(1f, 0f, 0f), floatArrayOf(5f, 0f, 0f)))
    }

    @Test
    fun `orthogonal vectors score zero`() {
        assertClose(0f, cosineSimilarity(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f)))
    }

    @Test
    fun `opposite vectors score minus one`() {
        assertClose(-1f, cosineSimilarity(floatArrayOf(1f, 1f), floatArrayOf(-1f, -1f)))
    }

    @Test
    fun `an all-zero vector scores zero`() {
        assertEquals(0f, cosineSimilarity(floatArrayOf(0f, 0f), floatArrayOf(1f, 1f)))
    }

    @Test
    fun `vectors of different dimensions are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            cosineSimilarity(floatArrayOf(1f, 2f), floatArrayOf(1f, 2f, 3f))
        }
    }
}
