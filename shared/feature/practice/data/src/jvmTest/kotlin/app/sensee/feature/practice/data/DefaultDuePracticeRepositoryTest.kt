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
}
