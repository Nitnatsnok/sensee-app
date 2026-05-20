package app.sensee.feature.practice.data.local

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.model.SrsCardSnapshot
import app.sensee.srs.core.model.SrsCardState
import kotlinx.coroutines.test.runTest
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class PracticeSrsStorageTest {
    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    @Test
    fun `get due cards returns persisted cards scheduled before now ordered by due time`() =
        runTest {
            val storage = PracticeSrsStorage(FakeDbProvider(freshDatabase()))
            val now = Instant.parse("2026-05-19T10:00:00Z")
            storage.saveCard(card("future", SrsCardState.Review, now + 1.days))
            storage.saveCard(card("due-later", SrsCardState.Review, now))
            storage.saveCard(card("due-first", SrsCardState.Learning, now - 1.days))
            storage.saveCard(card("new-card", SrsCardState.New, dueAt = null))
            storage.saveCard(card("suspended", SrsCardState.Suspended, now - 2.days))

            val due = storage.getDueCards(now = now, limit = 2)

            assertEquals(listOf("due-first", "due-later"), due.map { it.id.value })
        }

    @Test
    fun `save card if absent keeps existing review progress`() =
        runTest {
            val storage = PracticeSrsStorage(FakeDbProvider(freshDatabase()))
            val reviewed =
                card(
                    id = "card-1",
                    state = SrsCardState.Review,
                    dueAt = Instant.parse("2026-05-20T10:00:00Z"),
                    reviewCount = 1,
                )
            storage.saveCard(reviewed)

            val materialized = storage.saveCardIfAbsent(card("card-1", SrsCardState.New, dueAt = null))

            assertEquals(reviewed, materialized)
            assertEquals(reviewed, storage.getCard(SrsCardId("card-1")))
        }

    private fun freshDatabase(): SenseeDatabase {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY,
                Properties(),
                SenseeDatabase.Schema.synchronous(),
            )
        return SenseeDatabase(driver)
    }

    private fun card(
        id: String,
        state: SrsCardState,
        dueAt: Instant?,
        reviewCount: Int = 0,
    ): SrsCardSnapshot =
        SrsCardSnapshot(
            id = SrsCardId(id),
            state = state,
            dueAt = dueAt,
            lastReviewedAt = null,
            scheduledInterval = null,
            reviewCount = reviewCount,
            lapseCount = 0,
            stepIndex = null,
            algorithmState = null,
            algorithm = null,
            parametersId = null,
        )
}
