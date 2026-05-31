package app.sensee.core.compose.haptics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class HapticFeedbackOnTriggerTest {
    private class RecordingHapticFeedback : HapticFeedback {
        var count: Int = 0
            private set

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            count += 1
        }
    }

    @Test
    fun `null trigger never fires`() =
        runComposeUiTest {
            val recorder = RecordingHapticFeedback()
            setContent {
                HapticFeedbackOnTrigger(
                    trigger = null,
                    haptics = SenseeHaptics(recorder, enabled = true),
                )
            }
            waitForIdle()

            assertEquals(0, recorder.count)
        }

    @Test
    fun `crossing the threshold fires once and an unrelated recomposition does not refire`() =
        runComposeUiTest {
            val recorder = RecordingHapticFeedback()
            val haptics = SenseeHaptics(recorder, enabled = true)
            var trigger by mutableStateOf<String?>(null)
            var tick by mutableStateOf(0)
            setContent {
                // Read `tick` so bumping it recomposes this scope without changing `trigger`.
                tick.let { HapticFeedbackOnTrigger(trigger = trigger, haptics = haptics) }
            }
            waitForIdle()

            trigger = "start"
            waitForIdle()
            assertEquals(1, recorder.count)

            tick += 1
            waitForIdle()
            assertEquals(1, recorder.count)
        }

    @Test
    fun `dropping back to null re-arms the next crossing`() =
        runComposeUiTest {
            val recorder = RecordingHapticFeedback()
            val haptics = SenseeHaptics(recorder, enabled = true)
            var trigger by mutableStateOf<String?>(null)
            setContent { HapticFeedbackOnTrigger(trigger = trigger, haptics = haptics) }
            waitForIdle()

            trigger = "start"
            waitForIdle()
            trigger = null
            waitForIdle()
            trigger = "start"
            waitForIdle()

            assertEquals(2, recorder.count)
        }

    @Test
    fun `switching to a different non-null trigger fires again`() =
        runComposeUiTest {
            val recorder = RecordingHapticFeedback()
            val haptics = SenseeHaptics(recorder, enabled = true)
            var trigger by mutableStateOf<String?>("start")
            setContent { HapticFeedbackOnTrigger(trigger = trigger, haptics = haptics) }
            waitForIdle()

            trigger = "end"
            waitForIdle()

            assertEquals(2, recorder.count)
        }

    @Test
    fun `disabled haptics stays silent on crossing`() =
        runComposeUiTest {
            val recorder = RecordingHapticFeedback()
            var trigger by mutableStateOf<String?>(null)
            setContent {
                HapticFeedbackOnTrigger(trigger = trigger, haptics = SenseeHaptics(recorder, enabled = false))
            }
            waitForIdle()

            trigger = "start"
            waitForIdle()

            assertEquals(0, recorder.count)
        }

    @Test
    fun `threshold crossing fires once while progress stays above rearm threshold`() =
        runComposeUiTest {
            val recorder = RecordingHapticFeedback()
            val haptics = SenseeHaptics(recorder, enabled = true)
            var progress by mutableStateOf<Float?>(null)
            setContent { HapticFeedbackOnThresholdCrossing(progress = progress, haptics = haptics) }
            waitForIdle()

            progress = 1f
            waitForIdle()
            progress = 0.95f
            waitForIdle()
            progress = 1f
            waitForIdle()

            assertEquals(1, recorder.count)
        }

    @Test
    fun `threshold crossing re-arms after progress drops below hysteresis`() =
        runComposeUiTest {
            val recorder = RecordingHapticFeedback()
            val haptics = SenseeHaptics(recorder, enabled = true)
            var progress by mutableStateOf<Float?>(null)
            setContent { HapticFeedbackOnThresholdCrossing(progress = progress, haptics = haptics) }
            waitForIdle()

            progress = 1f
            waitForIdle()
            progress = 0.85f
            waitForIdle()
            progress = 1f
            waitForIdle()

            assertEquals(2, recorder.count)
        }

    @Test
    fun `inactive threshold progress re-arms the next crossing`() =
        runComposeUiTest {
            val recorder = RecordingHapticFeedback()
            val haptics = SenseeHaptics(recorder, enabled = true)
            var progress by mutableStateOf<Float?>(null)
            setContent { HapticFeedbackOnThresholdCrossing(progress = progress, haptics = haptics) }
            waitForIdle()

            progress = 1f
            waitForIdle()
            progress = null
            waitForIdle()
            progress = 1f
            waitForIdle()

            assertEquals(2, recorder.count)
        }
}
