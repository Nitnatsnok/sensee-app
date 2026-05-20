package app.sensee.tts.system

import app.sensee.core.platform.PlatformContext
import app.sensee.tts.core.Speaker
import app.sensee.tts.core.SpeechHandle
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.SpeechState
import app.sensee.tts.core.TtsError
import kotlinx.coroutines.CoroutineScope

internal interface WebSpeechSynthesis {
    fun createUtterance(text: String): WebSpeechUtterance

    fun speak(utterance: WebSpeechUtterance)

    fun cancel()
}

internal interface WebSpeechUtterance {
    var lang: String
    var rate: Double

    fun onStart(listener: () -> Unit)

    fun onEnd(listener: () -> Unit)

    fun onError(listener: (String?) -> Unit)
}

internal expect fun webSpeechSynthesisOrNull(): WebSpeechSynthesis?

private fun mapSpeechError(
    code: String?,
    request: SpeechRequest,
): TtsError =
    if (code == "language-unavailable" || code == "voice-unavailable") {
        TtsError.NoVoiceForLocale(request.locale)
    } else {
        TtsError.Unknown("Web Speech error: ${code ?: "unknown"}")
    }

internal class WebSpeechSpeaker : Speaker {
    private val synthesis: WebSpeechSynthesis? = webSpeechSynthesisOrNull()
    private var current: MutableSpeechHandle? = null

    override fun speak(request: SpeechRequest): SpeechHandle {
        val synth =
            synthesis
                ?: return MutableSpeechHandle(
                    initialState = SpeechState.Failed(TtsError.Unsupported("Web Speech API unavailable")),
                )
        val handle = MutableSpeechHandle(initialState = SpeechState.Loading) { synth.cancel() }
        // Move any prior in-flight handle to a terminal state before losing the
        // reference; otherwise callers still holding it would see Loading forever.
        current?.cancel()
        current = handle
        val utterance = synth.createUtterance(request.text)
        utterance.lang = request.locale.bcp47
        utterance.rate = request.rate.multiplier.toDouble()
        utterance.onStart { handle.update(SpeechState.Speaking) }
        utterance.onEnd { handle.update(SpeechState.Done) }
        utterance.onError { code ->
            handle.update(SpeechState.Failed(mapSpeechError(code, request)))
        }
        synth.cancel()
        synth.speak(utterance)
        return handle
    }

    override fun stop() {
        synthesis?.cancel()
        current?.update(SpeechState.Failed(TtsError.Cancelled()))
        current = null
    }
}

public actual fun systemSpeaker(
    context: PlatformContext,
    scope: CoroutineScope,
): Speaker = WebSpeechSpeaker()
