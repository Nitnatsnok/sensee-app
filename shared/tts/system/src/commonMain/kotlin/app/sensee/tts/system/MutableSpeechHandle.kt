package app.sensee.tts.system

import app.sensee.tts.core.SpeechHandle
import app.sensee.tts.core.SpeechState
import app.sensee.tts.core.TtsError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update

internal class MutableSpeechHandle(
    initialState: SpeechState = SpeechState.Idle,
    private val onCancel: () -> Unit = {},
) : SpeechHandle {
    override val state: StateFlow<SpeechState>
        field = MutableStateFlow(initialState)

    fun update(next: SpeechState) {
        state.update { next }
    }

    override fun cancel() {
        // Atomic check-then-set: a plain `mutable.value` read-modify-write could let two
        // concurrent cancel()/update() callers both pass the terminal-state guard. Flip the
        // state with getAndUpdate and fire onCancel() exactly once — for the single caller
        // that observed a non-terminal previous state.
        val previous =
            state.getAndUpdate { current ->
                if (current is SpeechState.Done || current is SpeechState.Failed) {
                    current
                } else {
                    SpeechState.Failed(TtsError.Cancelled())
                }
            }
        if (previous !is SpeechState.Done && previous !is SpeechState.Failed) {
            onCancel()
        }
    }
}
