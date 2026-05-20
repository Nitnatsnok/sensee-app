package app.sensee.tts.playback

import app.sensee.tts.core.AudioClip
import app.sensee.tts.core.AudioFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

/**
 * Plays a single audio clip to completion or until [stop] is invoked.
 *
 * Implementations are platform-specific and instantiated via [AudioPlayerFactory].
 * One instance is meant to play one clip at a time — if you need overlapping
 * playback, create multiple instances.
 */
public interface AudioPlayer {
    public suspend fun play(
        clip: AudioClip,
        rateMultiplier: Float = 1.0f,
    )

    /**
     * Plays audio as [chunks] arrive, so the first audio is heard before the
     * full clip has been downloaded/generated. The default buffers the whole
     * stream and falls back to [play], so platforms that cannot stream keep
     * working unchanged; only platforms that override this gain the latency win.
     *
     * The label [AudioFormat.Mp3] is inert here: every player decodes by
     * sniffing the container from the bytes, and this buffered clip is never
     * cached — it goes straight to [play].
     */
    public suspend fun playStream(
        chunks: Flow<ByteArray>,
        rateMultiplier: Float = 1.0f,
    ) {
        val bytes = chunks.toList().fold(ByteArray(0)) { acc, next -> acc + next }
        play(AudioClip(bytes = bytes, format = AudioFormat.Mp3), rateMultiplier)
    }

    public fun stop()

    public fun release()
}

public interface AudioPlayerFactory {
    public fun create(): AudioPlayer
}
