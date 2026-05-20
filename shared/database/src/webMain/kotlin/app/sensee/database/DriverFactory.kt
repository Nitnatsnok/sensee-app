package app.sensee.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver
import app.sensee.core.platform.PlatformEnvironment

public actual class DriverFactory actual constructor(
    @Suppress("UnusedPrivateProperty") platformEnvironment: PlatformEnvironment,
) {
    public actual suspend fun createDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver {
        val driver = createDefaultWebWorkerDriver()
        schema.create(driver).await()
        return driver
    }
}
