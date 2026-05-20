package app.sensee.tts.core

import kotlinx.coroutines.flow.StateFlow

public interface SpeechHandle {
    public val state: StateFlow<SpeechState>

    public fun cancel()
}
