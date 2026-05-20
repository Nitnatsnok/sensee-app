package app.sensee.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.observability.logging.NoOpAppLogger

/**
 * A fresh in-memory [SenseeDatabase] with the current schema reconciled in.
 * Shared by the query tests so the JdbcSqliteDriver + [reconcileDevSchema]
 * setup lives in one place.
 */
internal suspend fun freshInMemoryDatabase(): SenseeDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    reconcileDevSchema(driver, SenseeDatabase.Schema, NoOpAppLogger)
    return SenseeDatabase(driver)
}
