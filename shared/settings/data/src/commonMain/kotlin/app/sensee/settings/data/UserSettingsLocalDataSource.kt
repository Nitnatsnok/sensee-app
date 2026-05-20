package app.sensee.settings.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.database.User_setting
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.settings.domain.UserSettingsCategory
import app.sensee.settings.domain.UserSettingsScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock

@SingleIn(AppScope::class)
@Inject
public class UserSettingsLocalDataSource(
    private val databaseProvider: SenseeDatabaseProvider,
    private val dispatchers: AppDispatchers,
    private val clock: Clock,
) {
    internal fun observeRows(scope: UserSettingsScope): Flow<List<User_setting>> =
        flow {
            val database = databaseProvider.database()
            emitAll(
                database.userSettingEntityQueries
                    .selectSettingsByScope(scope.value)
                    .asFlow()
                    .mapToList(dispatchers.io),
            )
        }

    internal suspend fun selectRows(scope: UserSettingsScope): List<User_setting> =
        databaseProvider
            .database()
            .userSettingEntityQueries
            .selectSettingsByScope(scope.value)
            .awaitAsList()

    internal suspend fun upsertEntries(
        scope: UserSettingsScope,
        entries: List<UserSettingEntry>,
    ) {
        val database = databaseProvider.database()
        val updatedAtEpochMs = clock.now().toEpochMilliseconds()
        database.transaction {
            entries.forEach { entry ->
                database.userSettingEntityQueries.upsertSetting(
                    scope = scope.value,
                    category = entry.category.storageName,
                    key = entry.key,
                    value_json = entry.valueJson,
                    updated_at_epoch_ms = updatedAtEpochMs,
                    schema_version = CURRENT_SCHEMA_VERSION,
                )
            }
        }
    }

    internal suspend fun deleteEntry(
        scope: UserSettingsScope,
        category: UserSettingsCategory,
        key: String,
    ) {
        databaseProvider
            .database()
            .userSettingEntityQueries
            .deleteSetting(scope = scope.value, category = category.storageName, key = key)
    }
}
