package app.sensee.ui.learningDeck

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.collections.immutable.persistentSetOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LearningDeckSwipeStateTest {
    @Test
    fun calculates_end_progress_for_ltr_horizontal_drag() {
        val swipeState =
            calculateLearningDeckSwipeState(
                offset = Offset(40f, 10f),
                thresholdPx = 100f,
                layoutDirection = LayoutDirection.Ltr,
                allowedDirections =
                    persistentSetOf(
                        LearningSwipeDirection.Start,
                        LearningSwipeDirection.End,
                        LearningSwipeDirection.Up,
                        LearningSwipeDirection.Down,
                    ),
            )

        assertEquals(LearningDeckSwipePhase.Dragging, swipeState.phase)
        assertTrue(swipeState.isDragging)
        assertTrue(swipeState.isActive)
        assertTrue(!swipeState.isAnimating)
        assertEquals(LearningSwipeDirection.End, swipeState.direction)
        assertEquals(0.4f, swipeState.progress, absoluteTolerance = 0.0001f)
        assertEquals(0.4f, swipeState.endProgress, absoluteTolerance = 0.0001f)
        assertEquals(0f, swipeState.startProgress, absoluteTolerance = 0.0001f)
        assertEquals(0f, swipeState.upProgress, absoluteTolerance = 0.0001f)
        assertEquals(0.1f, swipeState.downProgress, absoluteTolerance = 0.0001f)
    }

    @Test
    fun pinned_state_preserves_phase_and_direction_for_settling_back() {
        val swipeState =
            createPinnedLearningDeckSwipeState(
                phase = LearningDeckSwipePhase.SettlingBack,
                direction = LearningSwipeDirection.Start,
                progress = 0.18f,
            )

        assertEquals(LearningDeckSwipePhase.SettlingBack, swipeState.phase)
        assertTrue(swipeState.isSettlingBack)
        assertTrue(swipeState.isAnimating)
        assertTrue(swipeState.isActive)
        assertEquals(LearningSwipeDirection.Start, swipeState.direction)
        assertEquals(0.18f, swipeState.progress, absoluteTolerance = 0.0001f)
        assertEquals(0.18f, swipeState.startProgress, absoluteTolerance = 0.0001f)
        assertEquals(0f, swipeState.endProgress, absoluteTolerance = 0.0001f)
        assertEquals(0f, swipeState.upProgress, absoluteTolerance = 0.0001f)
        assertEquals(0f, swipeState.downProgress, absoluteTolerance = 0.0001f)
        assertTrue(!swipeState.isIdle)
    }

    @Test
    fun idle_state_reports_idle_phase() {
        assertEquals(LearningDeckSwipePhase.Idle, LearningDeckSwipeState.Idle.phase)
        assertNull(LearningDeckSwipeState.Idle.direction)
        assertTrue(LearningDeckSwipeState.Idle.isIdle)
        assertTrue(!LearningDeckSwipeState.Idle.isActive)
        assertTrue(!LearningDeckSwipeState.Idle.isAnimating)
    }

    @Test
    fun rtl_positive_horizontal_drag_maps_to_start_progress() {
        val swipeState =
            calculateLearningDeckSwipeState(
                offset = Offset(30f, 0f),
                thresholdPx = 100f,
                layoutDirection = LayoutDirection.Rtl,
                allowedDirections =
                    persistentSetOf(
                        LearningSwipeDirection.Start,
                        LearningSwipeDirection.End,
                    ),
            )

        assertEquals(LearningDeckSwipePhase.Dragging, swipeState.phase)
        assertTrue(swipeState.isDragging)
        assertEquals(LearningSwipeDirection.Start, swipeState.direction)
        assertEquals(0.3f, swipeState.progress, absoluteTolerance = 0.0001f)
        assertEquals(0.3f, swipeState.startProgress, absoluteTolerance = 0.0001f)
        assertEquals(0f, swipeState.endProgress, absoluteTolerance = 0.0001f)
    }
}
