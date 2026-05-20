package app.sensee.ui.learningDeck

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.collections.immutable.persistentSetOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LearningSwipeDirectionResolutionTest {
    private val allDirections =
        persistentSetOf(
            LearningSwipeDirection.Start,
            LearningSwipeDirection.End,
            LearningSwipeDirection.Up,
            LearningSwipeDirection.Down,
        )

    private fun releaseSpec(): SwipeReleaseSpec =
        SwipeReleaseSpec(
            thresholdPx = 100f,
            flingVelocityThresholdPx = 900f,
            layoutDirection = LayoutDirection.Ltr,
            allowedDirections = allDirections,
        )

    @Test
    fun `offset below threshold does not resolve a direction`() {
        val direction =
            resolveSwipeDirectionFromOffset(
                offset = Offset(40f, 10f),
                thresholdPx = 100f,
                layoutDirection = LayoutDirection.Ltr,
                allowedDirections = allDirections,
            )

        assertNull(direction)
    }

    @Test
    fun `horizontal offset past threshold resolves end in ltr`() {
        val direction =
            resolveSwipeDirectionFromOffset(
                offset = Offset(140f, 20f),
                thresholdPx = 100f,
                layoutDirection = LayoutDirection.Ltr,
                allowedDirections = allDirections,
            )

        assertEquals(LearningSwipeDirection.End, direction)
    }

    @Test
    fun `positive horizontal offset maps to start in rtl`() {
        val direction =
            resolveSwipeDirectionFromOffset(
                offset = Offset(140f, 0f),
                thresholdPx = 100f,
                layoutDirection = LayoutDirection.Rtl,
                allowedDirections = allDirections,
            )

        assertEquals(LearningSwipeDirection.Start, direction)
    }

    @Test
    fun `diagonal tie resolves to the horizontal axis`() {
        val direction =
            resolveSwipeDirectionFromOffset(
                offset = Offset(120f, 120f),
                thresholdPx = 100f,
                layoutDirection = LayoutDirection.Ltr,
                allowedDirections = allDirections,
            )

        assertEquals(LearningSwipeDirection.End, direction)
    }

    @Test
    fun `disallowed resolved direction is rejected`() {
        val direction =
            resolveSwipeDirectionFromOffset(
                offset = Offset(140f, 0f),
                thresholdPx = 100f,
                layoutDirection = LayoutDirection.Ltr,
                allowedDirections = persistentSetOf(LearningSwipeDirection.Up),
            )

        assertNull(direction)
    }

    @Test
    fun `fast fling under the distance threshold still completes the swipe`() {
        val direction =
            resolveSwipeDirectionFromRelease(
                offset = Offset(20f, 0f),
                velocity = Offset(2000f, 0f),
                release = releaseSpec(),
            )

        assertEquals(LearningSwipeDirection.End, direction)
    }

    @Test
    fun `slow drag under the distance threshold settles back`() {
        val direction =
            resolveSwipeDirectionFromRelease(
                offset = Offset(20f, 0f),
                velocity = Offset(50f, 0f),
                release = releaseSpec(),
            )

        assertNull(direction)
    }

    @Test
    fun `distance past threshold completes even without fling velocity`() {
        val direction =
            resolveSwipeDirectionFromRelease(
                offset = Offset(0f, 180f),
                velocity = Offset(0f, 30f),
                release = releaseSpec(),
            )

        assertEquals(LearningSwipeDirection.Down, direction)
    }

    @Test
    fun `swipe exit target leaves the viewport horizontally in ltr`() {
        val target =
            calculateSwipeExitTarget(
                offset = Offset(120f, 15f),
                direction = LearningSwipeDirection.End,
                containerSize = IntSize(width = 400, height = 800),
                layoutDirection = LayoutDirection.Ltr,
                exitDistanceMultiplier = 1.35f,
            )

        assertEquals(400f * 1.35f, target.x, absoluteTolerance = 0.0001f)
        assertEquals(15f, target.y, absoluteTolerance = 0.0001f)
    }

    @Test
    fun `swipe exit target for up leaves the viewport vertically`() {
        val target =
            calculateSwipeExitTarget(
                offset = Offset(12f, -90f),
                direction = LearningSwipeDirection.Up,
                containerSize = IntSize(width = 400, height = 800),
                layoutDirection = LayoutDirection.Ltr,
                exitDistanceMultiplier = 1.35f,
            )

        assertEquals(12f, target.x, absoluteTolerance = 0.0001f)
        assertEquals(-800f * 1.35f, target.y, absoluteTolerance = 0.0001f)
    }

    @Test
    fun `swipe threshold uses the smallest container dimension`() {
        val threshold =
            calculateSwipeThresholdPx(
                containerSize = IntSize(width = 300, height = 900),
                thresholdFraction = 0.3f,
            )

        assertEquals(90f, threshold, absoluteTolerance = 0.0001f)
    }

    @Test
    fun `swipe threshold fraction is clamped to a sane range`() {
        val threshold =
            calculateSwipeThresholdPx(
                containerSize = IntSize(width = 1000, height = 1000),
                thresholdFraction = 5f,
            )

        assertTrue(threshold <= 1000f * 0.9f)
    }
}
