package app.sensee.feature.practice.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * Due-count coverage over a real in-memory database: only non-suspended cards due
 * at or before `now` count, and the observed flow reports the same.
 */
class DefaultDuePracticeRepositoryTest {
    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    private fun newDb(): SenseeDatabase =
        SenseeDatabase(
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY,
                Properties(),
                SenseeDatabase.Schema.synchronous(),
            ),
        )

    private suspend fun SenseeDatabase.insertCard(
        id: String,
        state: String,
        dueAtMs: Long?,
    ) {
        practiceSrsEntityQueries.upsertPracticeSrsCard(
            card_id = id,
            state = state,
            due_at_epoch_ms = dueAtMs,
            last_reviewed_at_epoch_ms = null,
            scheduled_interval_ms = null,
            review_count = 0,
            lapse_count = 0,
            step_index = null,
            algorithm_name = null,
            algorithm_version = null,
            fsrs_difficulty = null,
            fsrs_stability = null,
            parameters_id = null,
        )
    }

    @Test
    fun `countDue counts only non-suspended cards due at or before now`() =
        runTest {
            val db = newDb()
            db.insertCard("due", state = "Review", dueAtMs = 500L)
            db.insertCard("exactly-now", state = "Review", dueAtMs = 1_000L)
            db.insertCard("future", state = "Review", dueAtMs = 2_000L)
            db.insertCard("no-due", state = "New", dueAtMs = null)
            db.insertCard("suspended", state = "Suspended", dueAtMs = 500L)
            val repo = DefaultDuePracticeRepository(FakeDbProvider(db), immediateAppDispatchers())

            assertEquals(2, repo.countDue(Instant.fromEpochMilliseconds(1_000L)))
        }

    @Test
    fun `observeDueCount emits the current due count`() =
        runTest {
            val db = newDb()
            db.insertCard("due", state = "Review", dueAtMs = 500L)
            val repo = DefaultDuePracticeRepository(FakeDbProvider(db), immediateAppDispatchers())

            assertEquals(1, repo.observeDueCount(Instant.fromEpochMilliseconds(1_000L)).first())
        }

    @Test
    fun `dueCardIds returns due card ids ordered by due date ascending`() =
        runTest {
            val db = newDb()
            db.insertCard("later", state = "Review", dueAtMs = 900L)
            db.insertCard("earliest", state = "Review", dueAtMs = 100L)
            db.insertCard("middle", state = "Review", dueAtMs = 500L)
            val repo = DefaultDuePracticeRepository(FakeDbProvider(db), immediateAppDispatchers())

            val ids = repo.dueCardIds(Instant.fromEpochMilliseconds(1_000L), limit = 10)

            assertEquals(listOf("earliest", "middle", "later"), ids.map { it.value })
        }

    @Test
    fun `dueCardIds excludes suspended and not-yet-due cards`() =
        runTest {
            val db = newDb()
            db.insertCard("due", state = "Review", dueAtMs = 500L)
            db.insertCard("future", state = "Review", dueAtMs = 2_000L)
            db.insertCard("no-due", state = "New", dueAtMs = null)
            db.insertCard("suspended", state = "Suspended", dueAtMs = 500L)
            val repo = DefaultDuePracticeRepository(FakeDbProvider(db), immediateAppDispatchers())

            val ids = repo.dueCardIds(Instant.fromEpochMilliseconds(1_000L), limit = 10)

            assertEquals(listOf("due"), ids.map { it.value })
        }

    @Test
    fun `dueCardIds caps the result at the requested limit`() =
        runTest {
            val db = newDb()
            db.insertCard("first", state = "Review", dueAtMs = 100L)
            db.insertCard("second", state = "Review", dueAtMs = 200L)
            db.insertCard("third", state = "Review", dueAtMs = 300L)
            val repo = DefaultDuePracticeRepository(FakeDbProvider(db), immediateAppDispatchers())

            val ids = repo.dueCardIds(Instant.fromEpochMilliseconds(1_000L), limit = 2)

            assertEquals(listOf("first", "second"), ids.map { it.value })
        }

    @Test
    fun `dueCardIds orders cards sharing a due date by id so the cap is deterministic`() =
        runTest {
            val db = newDb()
            db.insertCard("c", state = "Review", dueAtMs = 100L)
            db.insertCard("a", state = "Review", dueAtMs = 100L)
            db.insertCard("b", state = "Review", dueAtMs = 100L)
            val repo = DefaultDuePracticeRepository(FakeDbProvider(db), immediateAppDispatchers())

            val ids = repo.dueCardIds(Instant.fromEpochMilliseconds(1_000L), limit = 2)

            assertEquals(listOf("a", "b"), ids.map { it.value })
        }

    @Test
    fun `dueCardIds returns empty for a non-positive limit`() =
        runTest {
            val db = newDb()
            db.insertCard("due", state = "Review", dueAtMs = 500L)
            val repo = DefaultDuePracticeRepository(FakeDbProvider(db), immediateAppDispatchers())

            assertEquals(emptyList(), repo.dueCardIds(Instant.fromEpochMilliseconds(1_000L), limit = 0))
        }
}
