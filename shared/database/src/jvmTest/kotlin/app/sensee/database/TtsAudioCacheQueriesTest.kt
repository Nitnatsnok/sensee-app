package app.sensee.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the LRU + budget queries the audio-cache eviction loop depends on:
 * `selectOldestKeys` is least-recently-accessed first, `touchAccess` reorders
 * it, and the aggregate/count drive the byte/entry budget. Eviction arithmetic
 * is thin glue over these — pinning the SQL is what guards the 50MB cache.
 */
class TtsAudioCacheQueriesTest {
    private fun clip(size: Int) = ByteArray(size) { 1 }

    @Test
    fun `oldest-keys is least-recently-accessed first and touch reorders it`() =
        runTest {
            val db = freshInMemoryDatabase()
            val q = db.ttsAudioCacheEntityQueries
            q.upsert("a", clip(10), "Mp3", 10, 1L, 1L)
            q.upsert("b", clip(20), "Mp3", 20, 2L, 2L)
            q.upsert("c", clip(30), "Mp3", 30, 3L, 3L)

            assertEquals(
                listOf("a", "b"),
                q.selectOldestKeys(2).awaitAsList().map { it.cache_key },
                "evict the two least-recently-accessed first",
            )

            q.touchAccess(99L, "a")

            assertEquals(
                listOf("b", "c"),
                q.selectOldestKeys(2).awaitAsList().map { it.cache_key },
                "a was just accessed, so it is no longer among the oldest",
            )
        }

    @Test
    fun `aggregate bytes and count drive the budget and deleteByKey shrinks both`() =
        runTest {
            val db = freshInMemoryDatabase()
            val q = db.ttsAudioCacheEntityQueries
            q.upsert("a", clip(10), "Mp3", 10, 1L, 1L)
            q.upsert("b", clip(20), "Mp3", 20, 2L, 2L)

            assertEquals(30L, q.aggregateBytes().awaitAsOneOrNull())
            assertEquals(2L, q.countEntries().awaitAsOneOrNull())

            q.deleteByKey("a")

            assertEquals(20L, q.aggregateBytes().awaitAsOneOrNull())
            assertEquals(1L, q.countEntries().awaitAsOneOrNull())
        }
}
