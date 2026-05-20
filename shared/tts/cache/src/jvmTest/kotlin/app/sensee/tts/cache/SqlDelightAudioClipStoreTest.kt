package app.sensee.tts.cache

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.tts.core.AudioClip
import app.sensee.tts.core.AudioFormat
import kotlinx.coroutines.test.runTest
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Pins eviction down to budget (not whole-cache wipe), LRU ordering via
 * `touchAccess`, and the byte budget — the failure-prone arithmetic guarding a
 * 50 MB on-device cache.
 */
class SqlDelightAudioClipStoreTest {
    private class MutableClock(
        var t: Long = 0L,
    ) : EpochMillisClock {
        override fun nowEpochMillis(): Long = t
    }

    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    private fun store(
        config: AudioCacheConfig,
        clock: MutableClock,
    ): SqlDelightAudioClipStore {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY,
                Properties(),
                SenseeDatabase.Schema.synchronous(),
            )
        return SqlDelightAudioClipStore(
            databaseProvider = FakeDbProvider(SenseeDatabase(driver)),
            dispatchers = immediateAppDispatchers(),
            config = config,
            clock = clock,
        )
    }

    private fun clip(size: Int) = AudioClip(bytes = ByteArray(size) { 1 }, format = AudioFormat.Mp3)

    @Test
    fun `exceeding the entry budget evicts only down to budget and not the whole cache`() =
        runTest {
            val clock = MutableClock()
            val store = store(AudioCacheConfig(maxBytes = 1_000_000, maxEntries = 2), clock)

            clock.t = 1
            store.put("a", clip(10))
            clock.t = 2
            store.put("b", clip(10))
            clock.t = 3
            store.put("c", clip(10))

            assertNull(store.get("a"), "the least-recently-accessed entry is evicted")
            assertNotNull(store.get("b"), "entries within budget are kept (no whole-cache wipe)")
            assertNotNull(store.get("c"))
        }

    @Test
    fun `a recently accessed entry is spared and the next-oldest is evicted`() =
        runTest {
            val clock = MutableClock()
            val store = store(AudioCacheConfig(maxBytes = 1_000_000, maxEntries = 2), clock)

            clock.t = 1
            store.put("a", clip(10))
            clock.t = 2
            store.put("b", clip(10))
            clock.t = 3
            store.get("a") // touches a -> b is now the oldest
            clock.t = 4
            store.put("c", clip(10))

            assertNull(store.get("b"), "b became least-recently-accessed and is evicted")
            assertNotNull(store.get("a"), "a was just accessed, so it survives")
            assertNotNull(store.get("c"))
        }

    @Test
    fun `the byte budget evicts oldest until within size keeping the rest`() =
        runTest {
            val clock = MutableClock()
            val store = store(AudioCacheConfig(maxBytes = 30, maxEntries = 1_000), clock)

            clock.t = 1
            store.put("a", clip(20))
            clock.t = 2
            store.put("b", clip(20)) // total 40 > 30 -> drop oldest (a), keep b

            assertNull(store.get("a"))
            assertNotNull(store.get("b"), "dropping one is enough; b is not over-evicted")
        }
}
