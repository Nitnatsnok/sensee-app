package app.sensee.tts.playback

import android.content.Context
import android.media.MediaDataSource
import android.media.MediaPlayer
import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.observability.logging.AppLogger
import app.sensee.core.platform.PlatformContext
import app.sensee.tts.core.AudioClip
import app.sensee.tts.core.TtsError
import app.sensee.tts.core.TtsException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class AndroidAudioPlayer(
    @Suppress("unused") private val context: Context,
) : AudioPlayer {
    private var player: MediaPlayer? = null

    override suspend fun play(
        clip: AudioClip,
        rateMultiplier: Float,
    ) {
        stop()
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        mediaPlayer.setDataSource(ByteArrayMediaDataSource(clip.bytes))
        suspendCancellableCoroutine { continuation ->
            mediaPlayer.setOnPreparedListener {
                try {
                    if (rateMultiplier != 1.0f) {
                        mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(rateMultiplier)
                    } else {
                        mediaPlayer.start()
                    }
                } catch (throwable: RuntimeException) {
                    continuation.resumeWithException(
                        TtsException(TtsError.Unknown(throwable.message), throwable),
                    )
                }
            }
            mediaPlayer.setOnCompletionListener { continuation.resume(Unit) }
            mediaPlayer.setOnErrorListener { _, what, extra ->
                continuation.resumeWithException(
                    TtsException(TtsError.Unknown("MediaPlayer error what=$what extra=$extra")),
                )
                true
            }
            continuation.invokeOnCancellation { stop() }
            mediaPlayer.prepareAsync()
        }
    }

    override fun stop() {
        player?.let { active ->
            try {
                if (active.isPlaying) active.stop()
                active.reset()
                active.release()
            } catch (_: RuntimeException) {
                // Best-effort teardown; callers use stop() from cancellation paths too.
            }
        }
        player = null
    }

    override fun release() {
        stop()
    }
}

private class ByteArrayMediaDataSource(
    private val data: ByteArray,
) : MediaDataSource() {
    override fun readAt(
        position: Long,
        buffer: ByteArray,
        offset: Int,
        size: Int,
    ): Int {
        if (position >= data.size) return -1
        val length = minOf(size.toLong(), data.size - position).toInt()
        System.arraycopy(data, position.toInt(), buffer, offset, length)
        return length
    }

    override fun getSize(): Long = data.size.toLong()

    override fun close() = Unit
}

internal class AndroidAudioPlayerFactory(
    private val context: Context,
) : AudioPlayerFactory {
    override fun create(): AudioPlayer = AndroidAudioPlayer(context)
}

public actual fun audioPlayerFactory(
    context: PlatformContext,
    logger: AppLogger,
    dispatchers: AppDispatchers,
): AudioPlayerFactory = AndroidAudioPlayerFactory(context.applicationContext)
