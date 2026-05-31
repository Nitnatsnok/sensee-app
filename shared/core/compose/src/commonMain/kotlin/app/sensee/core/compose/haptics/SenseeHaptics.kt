package app.sensee.core.compose.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * App-wide tactile-feedback entry point. Wraps the platform [HapticFeedback] together with the
 * user's "haptic feedback" preference, so callers fire feedback unconditionally and the gate
 * lives in one place. [perform] is a no-op when feedback is disabled or has no platform engine
 * (Desktop/JS/Wasm), so call sites never branch on availability themselves.
 */
@Immutable
public class SenseeHaptics(
    private val hapticFeedback: HapticFeedback?,
    private val enabled: Boolean,
) {
    public fun perform(type: HapticFeedbackType) {
        if (!enabled) return
        hapticFeedback?.performHapticFeedback(type)
    }

    public companion object {
        /** Inert instance used until the app environment provides a real one. */
        public val Disabled: SenseeHaptics = SenseeHaptics(hapticFeedback = null, enabled = false)
    }
}

/**
 * The active [SenseeHaptics] for the UI tree. Defaults to [SenseeHaptics.Disabled]; the
 * composition root (`AppComposeEnvironment`) provides a real instance built from
 * `LocalHapticFeedback` and the user setting.
 */
public val LocalSenseeHaptics: ProvidableCompositionLocal<SenseeHaptics> =
    staticCompositionLocalOf { SenseeHaptics.Disabled }

/**
 * Fires a one-shot haptic [type] while [trigger] is non-null: once on each transition to a new
 * non-null value, and once on first composition if [trigger] is already non-null.
 *
 * Pass the currently armed event identity as [trigger], or `null` while no event is armed. A
 * `null` trigger never fires, so returning to `null` re-arms the next non-null value and idle or
 * disabled states stay silent.
 */
@Composable
public fun <T> HapticFeedbackOnTrigger(
    trigger: T?,
    type: HapticFeedbackType = HapticFeedbackType.GestureThresholdActivate,
    haptics: SenseeHaptics = LocalSenseeHaptics.current,
) {
    val currentHaptics by rememberUpdatedState(haptics)
    LaunchedEffect(trigger) {
        if (trigger != null) {
            currentHaptics.perform(type)
        }
    }
}

/**
 * Fires one haptic pulse when [progress] crosses [threshold], then stays silent until progress
 * drops below [rearmThreshold]. A `null` progress means the interaction is inactive and re-arms
 * the next crossing.
 */
@Composable
public fun HapticFeedbackOnThresholdCrossing(
    progress: Float?,
    threshold: Float = 1f,
    rearmThreshold: Float = 0.9f,
    type: HapticFeedbackType = HapticFeedbackType.GestureThresholdActivate,
    haptics: SenseeHaptics = LocalSenseeHaptics.current,
) {
    val currentHaptics by rememberUpdatedState(haptics)
    val currentType by rememberUpdatedState(type)
    var armed by remember { mutableStateOf(true) }
    LaunchedEffect(progress, threshold, rearmThreshold) {
        when {
            progress == null -> armed = true
            progress < rearmThreshold -> armed = true
            armed && progress >= threshold -> {
                armed = false
                currentHaptics.perform(currentType)
            }
        }
    }
}
