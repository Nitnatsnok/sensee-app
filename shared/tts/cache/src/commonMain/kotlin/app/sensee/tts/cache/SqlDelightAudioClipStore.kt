package app.sensee.tts.cache

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.sensee.core.coroutines.AppDispatchers
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.tts.core.AudioClip
import app.sensee.tts.core.AudioClipStore
import app.sensee.tts.core.AudioFormat
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

public class SqlDelightAudioClipStore internal constructor(
    private val databaseProvider: SenseeDatabaseProvider,
    private val dispatchers: AppDispatchers,
    private val config: AudioCacheConfig,
    private val clock: EpochMillisClock,
) : AudioClipStore {
    private val evictionMutex = Mutex()

    override suspend fun get(key: String): AudioClip? =
        withContext(dispatchers.io) {
            val queries = databaseProvider.database().ttsAudioCacheEntityQueries
            val row = queries.selectByKey(key).awaitAsOneOrNull() ?: return@withContext null
            queries.touchAccess(clock.nowEpochMillis(), key)
            AudioClip(bytes = row.bytes, format = decodeFormat(row.format))
        }

    override suspend fun put(
        key: String,
        clip: AudioClip,
    ) {
        withContext(dispatchers.io) {
            val queries = databaseProvider.database().ttsAudioCacheEntityQueries
            val now = clock.nowEpochMillis()
            queries.upsert(
                cache_key = key,
                bytes = clip.bytes,
                format = clip.format.name,
                bytes_size = clip.bytes.size.toLong(),
                created_at_epoch_ms = now,
                last_accessed_at_epoch_ms = now,
            )
        }
        evictIfNeeded()
    }

    override suspend fun evictIfNeeded() {
        evictionMutex.withLock {
            withContext(dispatchers.io) {
                val queries = databaseProvider.database().ttsAudioCacheEntityQueries
                var totalBytes = queries.aggregateBytes().awaitAsOneOrNull() ?: 0L
                var entryCount = queries.countEntries().awaitAsOneOrNull() ?: 0L

                while (totalBytes > config.maxBytes || entryCount > config.maxEntries) {
                    // Fetch a batch (>= EVICTION_BATCH to avoid query thrashing),
                    // delete only down to budget so a small overflow trims to budget
                    // instead of clearing a cache with <= EVICTION_BATCH entries.
                    val toEvict = (entryCount - config.maxEntries).coerceAtLeast(EVICTION_BATCH)
                    val oldest = queries.selectOldestKeys(toEvict).awaitAsList()
                    if (oldest.isEmpty()) break
                    for (row in oldest) {
                        if (totalBytes <= config.maxBytes && entryCount <= config.maxEntries) break
                        queries.deleteByKey(row.cache_key)
                        totalBytes -= row.bytes_size
                        entryCount -= 1
                    }
                }
            }
        }
    }

    override suspend fun clear() {
        withContext(dispatchers.io) {
            databaseProvider.database().ttsAudioCacheEntityQueries.deleteAll()
        }
    }

    private fun decodeFormat(raw: String): AudioFormat =
        try {
            AudioFormat.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            AudioFormat.Mp3
        }

    public companion object {
        private const val EVICTION_BATCH: Long = 16L
    }
}

public object SqlDelightAudioClipStoreFactory {
    public fun create(
        databaseProvider: SenseeDatabaseProvider,
        dispatchers: AppDispatchers,
        config: AudioCacheConfig = AudioCacheConfig(),
    ): SqlDelightAudioClipStore =
        SqlDelightAudioClipStore(
            databaseProvider = databaseProvider,
            dispatchers = dispatchers,
            config = config,
            clock = SystemEpochMillisClock,
        )
}
