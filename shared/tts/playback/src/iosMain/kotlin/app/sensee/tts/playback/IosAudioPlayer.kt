@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package app.sensee.tts.playback

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.observability.logging.AppLogger
import app.sensee.core.platform.PlatformContext
import app.sensee.tts.core.AudioClip
import app.sensee.tts.core.TtsError
import app.sensee.tts.core.TtsException
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class IosAudioPlayer : AudioPlayer {
    private var player: AVAudioPlayer? = null
    private var delegate: AVAudioPlayerDelegateProtocol? = null

    override suspend fun play(
        clip: AudioClip,
        rateMultiplier: Float,
    ) {
        stop()
        val data = clip.bytes.toNSData()
        val newPlayer =
            memScoped {
                val errorRef = alloc<ObjCObjectVar<NSError?>>()
                val instance = AVAudioPlayer(data = data, error = errorRef.ptr)
                errorRef.value?.let { error ->
                    throw TtsException(
                        TtsError.Unknown("AVAudioPlayer init failed: ${error.localizedDescription}"),
                    )
                }
                instance
            }
        if (rateMultiplier != 1.0f) {
            newPlayer.enableRate = true
            newPlayer.rate = rateMultiplier
        }
        player = newPlayer
        suspendCancellableCoroutine { continuation ->
            val newDelegate =
                object : NSObject(), AVAudioPlayerDelegateProtocol {
                    override fun audioPlayerDidFinishPlaying(
                        player: AVAudioPlayer,
                        successfully: Boolean,
                    ) {
                        if (successfully) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(
                                TtsException(TtsError.Unknown("AVAudioPlayer finished unsuccessfully")),
                            )
                        }
                    }

                    override fun audioPlayerDecodeErrorDidOccur(
                        player: AVAudioPlayer,
                        error: NSError?,
                    ) {
                        continuation.resumeWithException(
                            TtsException(TtsError.Unknown(error?.localizedDescription)),
                        )
                    }
                }
            delegate = newDelegate
            newPlayer.setDelegate(newDelegate)
            continuation.invokeOnCancellation { stop() }
            if (!newPlayer.play()) {
                continuation.resumeWithException(
                    TtsException(TtsError.Unknown("AVAudioPlayer.play() returned false")),
                )
            }
        }
    }

    override fun stop() {
        player?.stop()
        player = null
        delegate = null
    }

    override fun release() = stop()
}

internal object IosAudioPlayerFactory : AudioPlayerFactory {
    override fun create(): AudioPlayer = IosAudioPlayer()
}

public actual fun audioPlayerFactory(
    context: PlatformContext,
    logger: AppLogger,
    dispatchers: AppDispatchers,
): AudioPlayerFactory = IosAudioPlayerFactory

private fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
