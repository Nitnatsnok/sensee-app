package app.sensee.tts.testKit

import app.sensee.tts.core.Speaker
import app.sensee.tts.core.SpeechHandle
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.SpeechState
import app.sensee.tts.core.TtsError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory [Speaker] that records every [SpeechRequest] it receives and
 * advances each handle to [SpeechState.Done] synchronously (or whatever final
 * state is configured).
 */
public class RecordingSpeaker(
    private val terminalState: SpeechState = SpeechState.Done,
) : Speaker {
    private val recorded = mutableListOf<SpeechRequest>()

    public val requests: List<SpeechRequest> get() = recorded.toList()
    public val lastRequest: SpeechRequest? get() = recorded.lastOrNull()

    override fun speak(request: SpeechRequest): SpeechHandle {
        recorded += request
        val state = MutableStateFlow<SpeechState>(terminalState)
        return object : SpeechHandle {
            override val state: StateFlow<SpeechState> = state.asStateFlow()

            override fun cancel() {
                state.update { SpeechState.Failed(TtsError.Cancelled()) }
            }
        }
    }

    override fun stop(): Unit = Unit

    public fun reset() {
        recorded.clear()
    }
}
