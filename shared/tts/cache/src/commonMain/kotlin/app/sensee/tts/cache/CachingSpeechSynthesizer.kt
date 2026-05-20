package app.sensee.tts.cache

import app.sensee.core.observability.logging.AppLogger
import app.sensee.tts.core.AudioClip
import app.sensee.tts.core.AudioClipStore
import app.sensee.tts.core.AudioFormat
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.SpeechSynthesizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Wraps a [SpeechSynthesizer] with a persistent [AudioClipStore].
 *
 * Lookup order on `synthesize`: cache hit → return stored bytes. Cache miss →
 * delegate, store the result, return. Errors from the delegate are not cached.
 *
 * [engineId] disambiguates entries when multiple synthesizers share the store
 * (e.g. local ElevenLabs vs. backend proxy producing different bytes for the
 * same request). Pick a stable string per engine version.
 */
public class CachingSpeechSynthesizer(
    private val delegate: SpeechSynthesizer,
    private val store: AudioClipStore,
    private val engineId: String,
    logger: AppLogger,
) : SpeechSynthesizer {
    private val log = logger.tag("TtsCache")

    override suspend fun synthesize(request: SpeechRequest): AudioClip {
        val key = request.toCacheKey(engineId)
        store.get(key)?.let {
            log.debug { "cache hit ($engineId): ${it.bytes.size} bytes" }
            return it
        }
        log.debug { "cache miss ($engineId) → synthesize" }
        val clip = delegate.synthesize(request)
        store.put(key, clip)
        log.debug { "persisted ${clip.bytes.size} bytes ($engineId)" }
        return clip
    }

    /**
     * Cache hit → replay the stored bytes as a single chunk. Cache miss → tee:
     * forward each chunk to the consumer for low-latency playback while
     * accumulating, then persist the full clip once the stream completes
     * normally. A failed or cancelled stream never reaches [AudioClipStore.put],
     * so a partial clip is not cached.
     */
    override fun stream(request: SpeechRequest): Flow<ByteArray> =
        flow {
            val key = request.toCacheKey(engineId)
            val cached = store.get(key)
            if (cached != null) {
                log.debug { "cache hit ($engineId): replay ${cached.bytes.size} bytes" }
                emit(cached.bytes)
                return@flow
            }
            log.debug { "cache miss ($engineId) → streaming" }
            val accumulated = mutableListOf<ByteArray>()
            delegate.stream(request).collect { chunk ->
                accumulated += chunk
                emit(chunk)
            }
            val full = ByteArray(accumulated.sumOf { it.size })
            var offset = 0
            accumulated.forEach { chunk ->
                chunk.copyInto(full, offset)
                offset += chunk.size
            }
            store.put(key, AudioClip(bytes = full, format = AudioFormat.Mp3))
            log.debug { "persisted ${full.size} bytes after stream ($engineId)" }
        }
}
