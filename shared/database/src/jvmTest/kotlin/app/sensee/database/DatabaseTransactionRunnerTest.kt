package app.sensee.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The neutral transaction seam `claim`/ingest rely on: writes spanning more than
 * one owner (here a lexicon `sense` row and a library `deck` row) commit or roll
 * back together. A failure after both writes must leave neither row — the
 * cross-store atomicity guarantee the claim-level tests cannot exercise, because
 * their in-memory SRS fake does not enlist in the `SenseeDatabase` transaction.
 */
class DatabaseTransactionRunnerTest {
    private class FakeProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    private suspend fun newDatabase(driver: SqlDriver): SenseeDatabase {
        SenseeDatabase.Schema.create(driver).await()
        return SenseeDatabase(driver)
    }

    // One write per owner inside the caller's transaction: a lexicon sense row and
    // a library deck row, mirroring the cross-owner shape of a claim/ingest.
    private suspend fun SenseeDatabase.writeSenseAndDeck() {
        senseQueries.upsert(
            sense_id = "s1",
            status = "Confirmed",
            sense_json = "{}",
            lemma_key = "k",
            content_key = "c",
            origin = "Personal",
            source_ref = null,
            cefr = null,
            unit_type = null,
            updated_at_epoch_ms = 1L,
        )
        catalogEntityQueries.insertDeckIfAbsent(
            id = "d1",
            title = "t",
            description = "d",
            card_count = 0L,
        )
    }

    @Test
    fun `a failure after a cross-owner write rolls back every row`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            val db = newDatabase(driver)
            val runner = DefaultDatabaseTransactionRunner(FakeProvider(db))

            assertFailsWith<IllegalStateException> {
                runner.transaction {
                    db.writeSenseAndDeck()
                    error("boom after both writes")
                }
            }

            assertEquals(0L, driver.count("sense", "sense_id", "s1"), "sense row must roll back")
            assertEquals(0L, driver.count("deck", "id", "d1"), "deck row must roll back with it")
        }

    @Test
    fun `a successful cross-owner transaction commits every row`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            val db = newDatabase(driver)
            val runner = DefaultDatabaseTransactionRunner(FakeProvider(db))

            runner.transaction { db.writeSenseAndDeck() }

            assertEquals(1L, driver.count("sense", "sense_id", "s1"))
            assertEquals(1L, driver.count("deck", "id", "d1"))
        }

    @Test
    fun `a nested transaction enlists and rolls back with the outer one`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            val db = newDatabase(driver)
            val runner = DefaultDatabaseTransactionRunner(FakeProvider(db))

            assertFailsWith<IllegalStateException> {
                runner.transaction {
                    db.writeSenseAndDeck()
                    // A nested write through the same runner must re-enter the held
                    // lock (not deadlock) and enlist, so it rolls back with the outer.
                    runner.transaction {
                        db.catalogEntityQueries.insertDeckIfAbsent(
                            id = "d2",
                            title = "t",
                            description = "d",
                            card_count = 0L,
                        )
                    }
                    error("boom after the nested write")
                }
            }

            assertEquals(0L, driver.count("sense", "sense_id", "s1"), "outer sense rolls back")
            assertEquals(0L, driver.count("deck", "id", "d1"), "outer deck rolls back")
            assertEquals(0L, driver.count("deck", "id", "d2"), "nested deck rolls back with the outer")
        }
}

private suspend fun SqlDriver.count(
    table: String,
    column: String,
    value: String,
): Long =
    executeQuery(
        identifier = null,
        sql = "SELECT COUNT(*) FROM $table WHERE $column = ?",
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0) ?: 0L)
        },
        parameters = 1,
        binders = { bindString(0, value) },
    ).await()
