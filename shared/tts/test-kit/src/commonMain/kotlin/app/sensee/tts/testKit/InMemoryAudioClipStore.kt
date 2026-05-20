package app.sensee.tts.testKit

import app.sensee.tts.core.AudioClip
import app.sensee.tts.core.AudioClipStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Test-only [AudioClipStore] that holds entries in a map. Supports a
 * size-bounded mode via [maxEntries] to exercise eviction-path tests against
 * decorators without standing up a real database.
 */
public class InMemoryAudioClipStore(
    private val maxEntries: Int = Int.MAX_VALUE,
) : AudioClipStore {
    private val mutex = Mutex()
    private val entries = linkedMapOf<String, AudioClip>()

    public val size: Int get() = entries.size

    override suspend fun get(key: String): AudioClip? =
        mutex.withLock {
            entries.remove(key)?.also { entries[key] = it }
        }

    override suspend fun put(
        key: String,
        clip: AudioClip,
    ) {
        mutex.withLock {
            entries.remove(key)
            entries[key] = clip
        }
        evictIfNeeded()
    }

    override suspend fun evictIfNeeded() {
        mutex.withLock {
            while (entries.size > maxEntries) {
                val oldest = entries.keys.iterator().next()
                entries.remove(oldest)
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock { entries.clear() }
    }

    public fun snapshot(): Map<String, AudioClip> = entries.toMap()
}
