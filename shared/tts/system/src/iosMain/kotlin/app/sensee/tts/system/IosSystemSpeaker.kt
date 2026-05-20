@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package app.sensee.tts.system

import app.sensee.core.platform.PlatformContext
import app.sensee.tts.core.Speaker
import app.sensee.tts.core.SpeechHandle
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.SpeechState
import app.sensee.tts.core.TtsError
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CoroutineScope
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate
import platform.darwin.NSObject

internal class IosSystemSpeaker(
    @Suppress("UNUSED_PARAMETER") scope: CoroutineScope,
) : Speaker {
    private val synthesizer = AVSpeechSynthesizer()
    private val handlesByUtterance = mutableMapOf<AVSpeechUtterance, MutableSpeechHandle>()
    private val delegate =
        object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
            @ObjCSignatureOverride
            override fun speechSynthesizer(
                synthesizer: AVSpeechSynthesizer,
                didStartSpeechUtterance: AVSpeechUtterance,
            ) {
                handlesByUtterance[didStartSpeechUtterance]?.update(SpeechState.Speaking)
            }

            @ObjCSignatureOverride
            override fun speechSynthesizer(
                synthesizer: AVSpeechSynthesizer,
                didFinishSpeechUtterance: AVSpeechUtterance,
            ) {
                handlesByUtterance.remove(didFinishSpeechUtterance)?.update(SpeechState.Done)
            }

            @ObjCSignatureOverride
            override fun speechSynthesizer(
                synthesizer: AVSpeechSynthesizer,
                didCancelSpeechUtterance: AVSpeechUtterance,
            ) {
                handlesByUtterance.remove(didCancelSpeechUtterance)?.update(
                    SpeechState.Failed(TtsError.Cancelled()),
                )
            }
        }

    init {
        synthesizer.setDelegate(delegate)
    }

    override fun speak(request: SpeechRequest): SpeechHandle {
        val voice = AVSpeechSynthesisVoice.voiceWithLanguage(request.locale.bcp47)
        if (voice == null) {
            return MutableSpeechHandle(initialState = SpeechState.Failed(TtsError.NoVoiceForLocale(request.locale)))
        }
        val utterance = AVSpeechUtterance(string = request.text)
        utterance.voice = voice
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * request.rate.multiplier
        val handle =
            MutableSpeechHandle(initialState = SpeechState.Loading) {
                synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
            }
        handlesByUtterance[utterance] = handle
        synthesizer.speakUtterance(utterance)
        return handle
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        handlesByUtterance.values.forEach { it.update(SpeechState.Failed(TtsError.Cancelled())) }
        handlesByUtterance.clear()
    }
}

public actual fun systemSpeaker(
    context: PlatformContext,
    scope: CoroutineScope,
): Speaker = IosSystemSpeaker(scope)
