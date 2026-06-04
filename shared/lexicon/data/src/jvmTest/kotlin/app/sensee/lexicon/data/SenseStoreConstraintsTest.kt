package app.sensee.lexicon.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.database.SenseeDatabase
import kotlinx.coroutines.test.runTest
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Schema-level invariants on the `sense` table, exercised through the raw
 * queries: a Service row must carry a `source_ref` (CHECK), and the partial
 * unique index keeps Service identity to one row per `source_ref`.
 */
class SenseStoreConstraintsTest {
    private fun newDb(): SenseeDatabase =
        SenseeDatabase(
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY,
                Properties(),
                SenseeDatabase.Schema.synchronous(),
            ),
        )

    private suspend fun SenseeDatabase.insertSense(
        senseId: String,
        origin: String,
        sourceRef: String?,
    ) {
        senseQueries.upsert(
            sense_id = senseId,
            status = "Confirmed",
            sense_json = "{}",
            lemma_key = "k",
            content_key = "c",
            origin = origin,
            source_ref = sourceRef,
            cefr = null,
            unit_type = null,
            updated_at_epoch_ms = 1L,
        )
    }

    @Test
    fun `a Service row without a source_ref violates the CHECK`() =
        runTest {
            val db = newDb()

            assertFailsWith<Throwable> { db.insertSense("a", origin = "Service", sourceRef = null) }
        }

    @Test
    fun `a Personal row may omit the source_ref`() =
        runTest {
            val db = newDb()

            db.insertSense("a", origin = "Personal", sourceRef = null)

            assertEquals(
                1,
                db.senseQueries
                    .selectAll()
                    .awaitAsList()
                    .size,
            )
        }

    @Test
    fun `two Service rows sharing a source_ref collapse to a single row`() =
        runTest {
            val db = newDb()

            db.insertSense("a", origin = "Service", sourceRef = "x")
            db.insertSense("b", origin = "Service", sourceRef = "x")

            val rows = db.senseQueries.selectAll().awaitAsList()
            assertEquals(1, rows.size)
            assertEquals("b", rows.single().sense_id, "the partial unique index keeps one row per source_ref")
        }
}
