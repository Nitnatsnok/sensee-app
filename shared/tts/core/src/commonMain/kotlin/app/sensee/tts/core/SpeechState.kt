package app.sensee.tts.core

public sealed interface SpeechState {
    public data object Idle : SpeechState

    public data object Loading : SpeechState

    public data object Speaking : SpeechState

    public data object Done : SpeechState

    public data class Failed(
        val error: TtsError,
    ) : SpeechState
}
