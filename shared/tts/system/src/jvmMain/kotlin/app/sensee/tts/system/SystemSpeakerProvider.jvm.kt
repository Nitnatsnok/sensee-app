package app.sensee.tts.system

import app.sensee.core.platform.PlatformContext
import app.sensee.tts.core.Speaker
import app.sensee.tts.core.SpeechHandle
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.SpeechState
import app.sensee.tts.core.TtsError
import kotlinx.coroutines.CoroutineScope

// Desktop has no built-in offline speech engine, and pulling in a heavyweight native
// TTS dependency (such as FreeTTS) is intentionally out of scope: provider TTS
// (ElevenLabs/OpenAI) already covers desktop through the implemented JVM audio player,
// so system TTS stays deliberately unsupported here.
internal class UnsupportedSystemSpeaker : Speaker {
    override fun speak(request: SpeechRequest): SpeechHandle =
        MutableSpeechHandle(
            initialState =
                SpeechState.Failed(
                    TtsError.Unsupported("System TTS not implemented for JVM target"),
                ),
        )

    override fun stop() = Unit
}

public actual fun systemSpeaker(
    context: PlatformContext,
    scope: CoroutineScope,
): Speaker = UnsupportedSystemSpeaker()
