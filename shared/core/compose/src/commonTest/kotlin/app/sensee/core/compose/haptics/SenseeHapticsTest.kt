package app.sensee.core.compose.haptics

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SenseeHapticsTest {
    private val threshold = HapticFeedbackType.GestureThresholdActivate

    private class RecordingHapticFeedback : HapticFeedback {
        val performed: MutableList<HapticFeedbackType> = mutableListOf()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performed += hapticFeedbackType
        }
    }

    @Test
    fun `enabled haptics forwards the requested feedback type`() {
        val recorder = RecordingHapticFeedback()
        val haptics = SenseeHaptics(hapticFeedback = recorder, enabled = true)

        haptics.perform(threshold)

        assertEquals(listOf(threshold), recorder.performed)
    }

    @Test
    fun `disabled haptics performs nothing`() {
        val recorder = RecordingHapticFeedback()
        val haptics = SenseeHaptics(hapticFeedback = recorder, enabled = false)

        haptics.perform(threshold)

        assertTrue(recorder.performed.isEmpty())
    }

    @Test
    fun `enabled haptics with no platform engine performs nothing`() {
        // Desktop/JS/Wasm have no HapticFeedback engine; the null branch must be a silent no-op.
        SenseeHaptics(hapticFeedback = null, enabled = true).perform(HapticFeedbackType.LongPress)
    }
}
