package app.sensee.settings.data

import app.sensee.core.database.User_setting
import app.sensee.core.secureStorage.SecureStorage
import app.sensee.core.secureStorage.SecureStorageKey
import app.sensee.settings.domain.UserSettingsCategory
import app.sensee.settings.domain.UserSettingsScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal const val LEGACY_AI_API_KEY_NAME: String = "ai_api_key"
internal const val LEGACY_TTS_API_KEY_NAME: String = "tts_api_key"

/**
 * One-shot fixup that moves api keys written by older builds (plaintext rows
 * in the `user_setting` table) into the platform secret vault, then deletes
 * the row so the plaintext disappears. Idempotent — once
 * [ensureMigrated] has succeeded in a process, it short-circuits.
 *
 * Best-effort by design: if either side errors (vault write, row delete),
 * the flag stays unset so the next read tries again. The repository's
 * canonical read path goes through the vault regardless, so a transient
 * failure does not lose the key.
 */
internal class LegacyAiSecretsMigration(
    private val localDataSource: UserSettingsLocalDataSource,
    private val secureStorage: SecureStorage,
    private val json: Json,
) {
    private val mutex = Mutex()
    private var done: Boolean = false

    suspend fun ensureMigrated(
        scope: UserSettingsScope,
        rows: List<User_setting>,
    ) {
        mutex.withLock {
            if (done) return
            val aiMigrated = migrateOne(scope, rows, LEGACY_AI_API_KEY_NAME, SecureStorageKey.AiApiKey)
            val ttsMigrated = migrateOne(scope, rows, LEGACY_TTS_API_KEY_NAME, SecureStorageKey.TtsApiKey)
            done = aiMigrated && ttsMigrated
        }
    }

    private suspend fun migrateOne(
        scope: UserSettingsScope,
        rows: List<User_setting>,
        legacyName: String,
        target: SecureStorageKey,
    ): Boolean {
        val legacy = legacyValue(rows, legacyName) ?: return true
        return try {
            if (secureStorage.read(target).isNullOrBlank()) {
                secureStorage.write(target, legacy)
            }
            localDataSource.deleteEntry(scope, UserSettingsCategory.Ai, legacyName)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            false
        }
    }

    private fun legacyValue(
        rows: List<User_setting>,
        legacyName: String,
    ): String? {
        val row =
            rows.firstOrNull {
                it.category == UserSettingsCategory.Ai.storageName && it.key == legacyName
            } ?: return null
        val decoded =
            try {
                json.decodeFromString(String.serializer().nullable, row.value_json)
            } catch (_: SerializationException) {
                null
            }
        return decoded?.takeIf { it.isNotBlank() }
    }
}
