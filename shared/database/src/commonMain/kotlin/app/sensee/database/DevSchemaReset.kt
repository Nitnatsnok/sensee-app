package app.sensee.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlSchema
import app.sensee.core.observability.logging.AppLogger

/**
 * Dev-only schema reset, driven by *actual schema drift* rather than the
 * SQLDelight schema version.
 *
 * The version SQLDelight exposes is `(highest .sqm number) + 1`; editing a `.sq`
 * file in place — the exact workflow this tool exists to support — never changes
 * it, so the driver calls neither `create` nor `migrate` and a version-gated
 * reset never fires. Instead we fingerprint the statements [SqlSchema.create]
 * emits and store that fingerprint in [MARKER_TABLE]. On launch, a changed
 * fingerprint means the `.sq` files moved: drop every user object and recreate.
 *
 * Erases all data on mismatch. Only call when `resetOnSchemaMigration = true`.
 */
internal suspend fun reconcileDevSchema(
    driver: SqlDriver,
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
    logger: AppLogger,
) {
    driver
        .execute(
            null,
            "CREATE TABLE IF NOT EXISTS $MARKER_TABLE (fingerprint TEXT NOT NULL)",
            0,
        ).await()

    val expected = schemaFingerprint(schema)
    val stored = readStoredFingerprint(driver)
    if (stored == expected) return

    logger.warn {
        "Dev DB reset: schema fingerprint changed " +
            "(${stored ?: "none"} -> $expected), recreating schema"
    }
    dropAllUserObjects(driver)
    schema.create(driver).await()
    driver
        .execute(
            null,
            "CREATE TABLE IF NOT EXISTS $MARKER_TABLE (fingerprint TEXT NOT NULL)",
            0,
        ).await()
    driver.execute(null, "DELETE FROM $MARKER_TABLE", 0).await()
    driver
        .execute(null, "INSERT INTO $MARKER_TABLE (fingerprint) VALUES (?)", 1) {
            bindString(0, expected)
        }.await()
}

internal suspend fun dropAllUserObjects(driver: SqlDriver) {
    // Drop in dependency order: triggers first (depend on tables and views),
    // then views (may reference tables), then tables (their indexes go with them).
    for (type in DROP_ORDER) {
        for (name in listUserObjects(driver, type)) {
            driver.execute(null, """DROP $type IF EXISTS "$name"""", 0).await()
        }
    }
}

internal suspend fun listUserObjects(
    driver: SqlDriver,
    type: String,
): List<String> =
    driver
        .executeQuery(
            identifier = null,
            sql =
                "SELECT name FROM sqlite_master " +
                    "WHERE type = ? " +
                    "AND name NOT LIKE 'sqlite_%' " +
                    "AND name != 'android_metadata'",
            mapper = { cursor ->
                // Drain the cursor synchronously inside the mapper — drivers close
                // it as soon as we return, so suspending past that point reads
                // garbage. Sync drivers (JDBC/Android/Native) return Value here;
                // we don't support this on async cursors.
                val names = mutableListOf<String>()
                while ((cursor.next() as QueryResult.Value).value) {
                    cursor.getString(0)?.let(names::add)
                }
                QueryResult.Value(names.toList())
            },
            parameters = 1,
            binders = { bindString(0, type) },
        ).await()

private suspend fun readStoredFingerprint(driver: SqlDriver): String? =
    driver
        .executeQuery(
            identifier = null,
            sql = "SELECT fingerprint FROM $MARKER_TABLE LIMIT 1",
            mapper = { cursor ->
                val value =
                    if ((cursor.next() as QueryResult.Value).value) {
                        cursor.getString(0)
                    } else {
                        null
                    }
                QueryResult.Value(value)
            },
            parameters = 0,
        ).await()

/**
 * Fingerprint of the schema's create script: run [SqlSchema.create] against a
 * driver that only records the emitted DDL, then hash the statements (FNV-1a,
 * 64-bit). Any `.sq` change shifts the emitted SQL and therefore the hash.
 */
private suspend fun schemaFingerprint(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): String {
    val recorder = RecordingSqlDriver()
    schema.create(recorder).await()
    var hash = FNV_OFFSET_BASIS
    for (statement in recorder.statements) {
        for (char in statement) {
            hash = (hash xor char.code.toLong()) * FNV_PRIME
        }
        hash = (hash xor SEPARATOR) * FNV_PRIME
    }
    return hash.toULong().toString(RADIX_HEX)
}

private class RecordingSqlDriver : SqlDriver {
    val statements: MutableList<String> = mutableListOf()

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<Long> {
        statements += sql
        return QueryResult.AsyncValue { 0L }
    }

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> = error("RecordingSqlDriver only records schema creation DDL")

    override fun newTransaction(): QueryResult<Transacter.Transaction> =
        error("RecordingSqlDriver does not support transactions")

    override fun currentTransaction(): Transacter.Transaction? = null

    override fun addListener(
        vararg queryKeys: String,
        listener: Query.Listener,
    ) = Unit

    override fun removeListener(
        vararg queryKeys: String,
        listener: Query.Listener,
    ) = Unit

    override fun notifyListeners(vararg queryKeys: String) = Unit

    override fun close() = Unit
}

private const val MARKER_TABLE = "sensee_dev_schema"
private val DROP_ORDER = listOf("trigger", "view", "table")
private const val FNV_OFFSET_BASIS = -3750763034362895579L // 0xcbf29ce484222325
private const val FNV_PRIME = 1099511628211L
private const val SEPARATOR = 0x1FL
private const val RADIX_HEX = 16
