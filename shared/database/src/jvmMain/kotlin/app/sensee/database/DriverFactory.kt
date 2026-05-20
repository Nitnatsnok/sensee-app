package app.sensee.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.platform.PlatformEnvironment
import java.io.File
import java.util.Properties

public actual class DriverFactory actual constructor(
    @Suppress("UnusedPrivateProperty") platformEnvironment: PlatformEnvironment,
) {
    public actual suspend fun createDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver {
        val databaseFile =
            File(
                System.getProperty("user.home"),
                ".sensee/sensee.db",
            )
        // sqlite-jdbc opens existing files but does not create missing parent
        // dirs — SQLITE_CANTOPEN on first launch.
        databaseFile.parentFile?.mkdirs()
        val driver: SqlDriver =
            JdbcSqliteDriver(
                url = "jdbc:sqlite:${databaseFile.absolutePath}",
                properties = Properties(),
                schema = schema.synchronous(),
            )
        return driver
    }
}
