package app.sensee.tts.playback

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.observability.logging.AppLogger
import app.sensee.core.platform.PlatformContext
import app.sensee.tts.core.AudioClip
import app.sensee.tts.core.TtsError
import app.sensee.tts.core.TtsException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal interface WebAudioElement {
    var src: String
    var playbackRate: Double

    fun play()

    fun pause()

    fun onEnded(listener: () -> Unit)

    fun onError(listener: () -> Unit)
}

internal expect fun newWebAudioElement(): WebAudioElement

internal class WebAudioPlayer : AudioPlayer {
    private var element: WebAudioElement? = null
    private var activeContinuation: CancellableContinuation<Unit>? = null

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun play(
        clip: AudioClip,
        rateMultiplier: Float,
    ) {
        stop()
        val dataUrl = "data:${audioMimeType(clip.format)};base64," + Base64.encode(clip.bytes)
        suspendCancellableCoroutine { continuation ->
            val audio = newWebAudioElement()
            element = audio
            activeContinuation = continuation
            audio.src = dataUrl
            audio.playbackRate = rateMultiplier.toDouble()
            audio.onEnded {
                if (continuation.isActive) continuation.resume(Unit)
            }
            audio.onError {
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        TtsException(TtsError.Unknown("HTMLAudioElement playback error")),
                    )
                }
            }
            continuation.invokeOnCancellation { stop() }
            audio.play()
        }
    }

    override fun stop() {
        val pending = activeContinuation
        activeContinuation = null
        element?.let { audio ->
            audio.pause()
            audio.src = ""
        }
        element = null
        // Resume a stop() called from outside the coroutine so the suspended play()
        // doesn't hang. invokeOnCancellation re-enters stop() after isActive flips,
        // so the guard prevents a double-resume.
        if (pending != null && pending.isActive) {
            pending.resumeWithException(TtsException(TtsError.Cancelled()))
        }
    }

    override fun release() = stop()
}

internal object WebAudioPlayerFactory : AudioPlayerFactory {
    override fun create(): AudioPlayer = WebAudioPlayer()
}

public actual fun audioPlayerFactory(
    context: PlatformContext,
    logger: AppLogger,
    dispatchers: AppDispatchers,
): AudioPlayerFactory = WebAudioPlayerFactory
