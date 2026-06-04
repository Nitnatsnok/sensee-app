package app.sensee.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.observability.logging.AppLogger
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DevSchemaResetTest {
    @Test
    fun `stale schema is reset even though the schema version never changed`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            // A database left behind by an earlier .sq edit: stale objects, and a
            // marker holding a fingerprint that no longer matches the schema. The
            // SQLDelight version is unchanged (no .sqm exists), so the old
            // version-gated path would never have fired here.
            driver.execSync("CREATE TABLE old_table (id INTEGER PRIMARY KEY)")
            driver.execSync("CREATE VIEW old_view AS SELECT id FROM old_table")
            driver.execSync("CREATE TABLE sensee_dev_schema (fingerprint TEXT NOT NULL)")
            driver.execSync("INSERT INTO sensee_dev_schema (fingerprint) VALUES ('stale')")

            reconcileDevSchema(driver, SenseeDatabase.Schema, recordingLogger)

            val tables = driver.listTables()
            assertFalse("old_table" in tables, "stale table should be dropped")
            assertFalse(
                "old_view" in driver.listObjects("view"),
                "stale view should be dropped",
            )
            assertTrue(
                "sense" in tables,
                "current schema table missing after reset",
            )
            assertEquals(1, recordedMessages.size, "exactly one reset should be logged")
        }

    @Test
    fun `matching fingerprint leaves existing data untouched on next launch`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

            reconcileDevSchema(driver, SenseeDatabase.Schema, recordingLogger)
            driver.execSync(
                "INSERT INTO deck (id, title, description, card_count) VALUES ('d1', 'hello', 'desc', 0)",
            )

            // Second launch, schema unchanged: must be a no-op, not a wipe.
            reconcileDevSchema(driver, SenseeDatabase.Schema, recordingLogger)

            assertEquals(
                1L,
                driver.countDecks(),
                "data was wiped despite an unchanged schema",
            )
            assertEquals(
                1,
                recordedMessages.size,
                "reset should run once, not on every launch",
            )
        }

    private val recordedMessages = mutableListOf<String>()
    private val recordingLogger: AppLogger =
        object : AppLogger {
            override fun tag(tag: String): AppLogger = this

            override fun verbose(
                throwable: Throwable?,
                message: () -> String,
            ) = Unit

            override fun debug(
                throwable: Throwable?,
                message: () -> String,
            ) = Unit

            override fun info(
                throwable: Throwable?,
                message: () -> String,
            ) = Unit

            override fun warn(
                throwable: Throwable?,
                message: () -> String,
            ) {
                recordedMessages += message()
            }

            override fun error(
                throwable: Throwable?,
                message: () -> String,
            ) = Unit
        }
}

private fun SqlDriver.execSync(sql: String) {
    val result = execute(null, sql, 0)
    check(result is QueryResult.Value) {
        "Expected synchronous JdbcSqliteDriver execute"
    }
}

private suspend fun SqlDriver.listObjects(type: String): List<String> =
    executeQuery(
        identifier = null,
        sql = "SELECT name FROM sqlite_master WHERE type = ? AND name NOT LIKE 'sqlite_%'",
        mapper = { cursor ->
            val names = mutableListOf<String>()
            while ((cursor.next() as QueryResult.Value).value) {
                cursor.getString(0)?.let(names::add)
            }
            QueryResult.Value(names.toList())
        },
        parameters = 1,
        binders = { bindString(0, type) },
    ).await()

private suspend fun SqlDriver.listTables(): List<String> = listObjects("table")

private suspend fun SqlDriver.countDecks(): Long =
    executeQuery(
        identifier = null,
        sql = "SELECT COUNT(*) FROM deck",
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0) ?: 0L)
        },
        parameters = 0,
    ).await()
