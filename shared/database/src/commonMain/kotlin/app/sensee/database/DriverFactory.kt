package app.sensee.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.sensee.core.observability.logging.AppLogger
import app.sensee.core.platform.PlatformEnvironment

public expect class DriverFactory(
    platformEnvironment: PlatformEnvironment,
) {
    public suspend fun createDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver
}

public suspend fun createDatabase(
    driverFactory: DriverFactory,
    config: DatabaseConfig,
    logger: AppLogger,
): SenseeDatabase {
    val driver = driverFactory.createDriver(SenseeDatabase.Schema)
    if (config.resetOnSchemaMigration) {
        reconcileDevSchema(driver, SenseeDatabase.Schema, logger)
    }
    return SenseeDatabase(driver)
}
