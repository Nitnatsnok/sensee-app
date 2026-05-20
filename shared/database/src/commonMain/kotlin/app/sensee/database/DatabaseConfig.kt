package app.sensee.database

/**
 * Runtime database settings, supplied by the host (Android app, desktop launcher,
 * iOS holder, web bootstrapper).
 *
 * @property resetOnSchemaMigration when `true`, a schema version mismatch causes
 *   the database to be wiped and recreated instead of running a migration. Intended
 *   for dev builds where writing throwaway migrations is wasteful. **Never enable
 *   in production — it destroys user data.**
 */
public data class DatabaseConfig(
    val resetOnSchemaMigration: Boolean = false,
)
