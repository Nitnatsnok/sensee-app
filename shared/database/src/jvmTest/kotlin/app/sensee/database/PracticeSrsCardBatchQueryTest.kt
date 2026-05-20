package app.sensee.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the set-based read that replaced the per-card N+1 on the deck-open path:
 * one query returns every present card and silently skips absent ids.
 */
class PracticeSrsCardBatchQueryTest {
    @Test
    fun `selects every present srs card in one query and skips absent ids`() =
        runTest {
            val db = freshInMemoryDatabase()
            insertCard(db, "a")
            insertCard(db, "b")
            insertCard(db, "c")

            val rows =
                db.practiceSrsEntityQueries
                    .selectPracticeSrsCardsByIds(listOf("a", "c", "missing"))
                    .awaitAsList()

            assertEquals(setOf("a", "c"), rows.map { it.card_id }.toSet())
        }

    private suspend fun insertCard(
        db: SenseeDatabase,
        id: String,
    ) {
        db.practiceSrsEntityQueries.upsertPracticeSrsCard(
            card_id = id,
            state = "New",
            due_at_epoch_ms = null,
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
}
