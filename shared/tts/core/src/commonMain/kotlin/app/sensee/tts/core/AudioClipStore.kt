package app.sensee.tts.core

/**
 * Persistent key-value store for synthesized audio clips.
 *
 * Implementations live alongside the modules that own a backing store
 * (`tts/cache` for the SQLDelight-backed default; `tts/test-kit` for an
 * in-memory fake).
 */
public interface AudioClipStore {
    public suspend fun get(key: String): AudioClip?

    public suspend fun put(
        key: String,
        clip: AudioClip,
    )

    public suspend fun evictIfNeeded()

    public suspend fun clear()
}
