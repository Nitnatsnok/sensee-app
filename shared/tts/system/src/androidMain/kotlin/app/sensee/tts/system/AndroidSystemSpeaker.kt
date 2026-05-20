package app.sensee.tts.system

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import app.sensee.core.platform.PlatformContext
import app.sensee.tts.core.Speaker
import app.sensee.tts.core.SpeechHandle
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.SpeechState
import app.sensee.tts.core.TtsError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

internal class AndroidSystemSpeaker(
    context: Context,
    private val scope: CoroutineScope,
) : Speaker {
    private val utteranceCounter = AtomicLong()
    private val pending = mutableMapOf<String, MutableSpeechHandle>()
    private val initialization = CompletableDeferred<Result<Unit>>()

    private val engine: TextToSpeech =
        TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                initialization.complete(Result.success(Unit))
            } else {
                initialization.complete(
                    Result.failure(IllegalStateException("TextToSpeech.init failed with status $status")),
                )
            }
        }

    init {
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    utteranceId?.let { id ->
                        synchronized(pending) { pending[id] }?.update(SpeechState.Speaking)
                    }
                }

                override fun onDone(utteranceId: String?) {
                    utteranceId?.let { id ->
                        val handle = synchronized(pending) { pending.remove(id) }
                        handle?.update(SpeechState.Done)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onError(utteranceId, TextToSpeech.ERROR)
                }

                override fun onError(
                    utteranceId: String?,
                    errorCode: Int,
                ) {
                    utteranceId?.let { id ->
                        val handle = synchronized(pending) { pending.remove(id) }
                        handle?.update(
                            SpeechState.Failed(TtsError.Unknown("TextToSpeech error code $errorCode")),
                        )
                    }
                }
            },
        )
    }

    override fun speak(request: SpeechRequest): SpeechHandle {
        val utteranceId = utteranceCounter.incrementAndGet().toString()
        val handle =
            MutableSpeechHandle(initialState = SpeechState.Loading) {
                engine.stop()
            }
        synchronized(pending) { pending[utteranceId] = handle }
        scope.launch {
            val result = initialization.await()
            if (result.isFailure) {
                synchronized(pending) { pending.remove(utteranceId) }
                handle.update(
                    SpeechState.Failed(
                        TtsError.Unknown(result.exceptionOrNull()?.message),
                    ),
                )
                return@launch
            }
            val locale = Locale.forLanguageTag(request.locale.bcp47)
            val localeAvailability = engine.setLanguage(locale)
            if (localeAvailability == TextToSpeech.LANG_MISSING_DATA ||
                localeAvailability == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                synchronized(pending) { pending.remove(utteranceId) }
                handle.update(SpeechState.Failed(TtsError.NoVoiceForLocale(request.locale)))
                return@launch
            }
            engine.setSpeechRate(request.rate.multiplier)
            val params = Bundle()
            val queued = engine.speak(request.text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (queued != TextToSpeech.SUCCESS) {
                synchronized(pending) { pending.remove(utteranceId) }
                handle.update(SpeechState.Failed(TtsError.Unknown("TextToSpeech.speak returned $queued")))
            }
        }
        return handle
    }

    override fun stop() {
        engine.stop()
        synchronized(pending) {
            pending.values.forEach { it.update(SpeechState.Failed(TtsError.Cancelled())) }
            pending.clear()
        }
    }
}

public actual fun systemSpeaker(
    context: PlatformContext,
    scope: CoroutineScope,
): Speaker = AndroidSystemSpeaker(context.applicationContext, scope)
