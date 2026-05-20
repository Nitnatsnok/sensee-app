package app.sensee.settings.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.secureStorage.SecureStorage
import app.sensee.core.secureStorage.SecureStorageException
import app.sensee.core.secureStorage.SecureStorageKey
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.settings.domain.AiProvider
import app.sensee.settings.domain.AiSettings
import app.sensee.settings.domain.TtsProvider
import app.sensee.settings.domain.UserSettingsCategory
import app.sensee.settings.domain.UserSettingsScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultUserSettingsRepositoryTest {
    private object FixedClock : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000L)
    }

    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    // In-memory stand-in for the platform secret vault. Mirrors the real
    // contract: reads return null for absent keys, writes are immediate.
    private class InMemorySecureStorage : SecureStorage {
        private val backing: MutableMap<String, String> = mutableMapOf()
        var failWrites: Boolean = false

        override suspend fun read(key: SecureStorageKey): String? = backing[key.value]

        override suspend fun write(
            key: SecureStorageKey,
            value: String?,
        ) {
            if (failWrites) {
                throw SecureStorageException("write failed")
            }
            if (value == null) backing.remove(key.value) else backing[key.value] = value
        }

        fun snapshot(): Map<String, String> = backing.toMap()
    }

    private class Harness {
        private val json = Json
        private val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY,
                Properties(),
                SenseeDatabase.Schema.synchronous(),
            )
        private val database = SenseeDatabase(driver)
        private val localDataSource =
            UserSettingsLocalDataSource(
                databaseProvider = FakeDbProvider(database),
                dispatchers = immediateAppDispatchers(),
                clock = FixedClock,
            )
        val secureStorage = InMemorySecureStorage()

        val repository = DefaultUserSettingsRepository(localDataSource, secureStorage, json)

        suspend fun upsertLegacyAiEntries(vararg entries: Pair<String, String?>) {
            localDataSource.upsertEntries(
                UserSettingsScope.Device,
                entries.map { (key, value) ->
                    UserSettingEntry(
                        category = UserSettingsCategory.Ai,
                        key = key,
                        valueJson = json.encodeToString(String.serializer().nullable, value),
                    )
                },
            )
        }

        suspend fun aiCategoryRowKeys(): Set<String> =
            localDataSource
                .selectRows(UserSettingsScope.Device)
                .filter { it.category == UserSettingsCategory.Ai.storageName }
                .map { it.key }
                .toSet()
    }

    @Test
    fun `AI settings round-trip preserves separate TTS key flag`() =
        runTest {
            val harness = Harness()
            val expected =
                AiSettings(
                    aiApiKey = "sk-ai",
                    aiProvider = AiProvider.OpenAi,
                    aiModel = "gpt-4o",
                    ttsApiKey = "sk-tts-separate",
                    ttsProvider = TtsProvider.OpenAi,
                    ttsModel = "tts-1",
                    ttsVoiceId = "alloy",
                    ttsSeparateKey = true,
                )

            harness.repository.updateAiSettings { expected }

            assertEquals(expected, harness.repository.readSettings().ai)
        }

    @Test
    fun `api keys live in secure storage and never in the user_setting table`() =
        runTest {
            val harness = Harness()

            harness.repository.updateAiSettings {
                AiSettings(
                    aiApiKey = "sk-ai",
                    aiProvider = AiProvider.OpenAi,
                    ttsApiKey = "sk-tts",
                    ttsProvider = TtsProvider.ElevenLabs,
                )
            }

            assertEquals("sk-ai", harness.secureStorage.snapshot()[SecureStorageKey.AiApiKey.value])
            assertEquals("sk-tts", harness.secureStorage.snapshot()[SecureStorageKey.TtsApiKey.value])
            assertTrue("ai_api_key" !in harness.aiCategoryRowKeys())
            assertTrue("tts_api_key" !in harness.aiCategoryRowKeys())
        }

    @Test
    fun `legacy plaintext api keys in user_setting are migrated into secure storage on first read`() =
        runTest {
            val harness = Harness()
            harness.upsertLegacyAiEntries(
                "ai_api_key" to "sk-legacy-ai",
                "ai_provider" to AiProvider.OpenAi.id,
                "tts_api_key" to "sk-legacy-tts",
                "tts_provider" to TtsProvider.OpenAi.id,
            )

            val ai = harness.repository.readSettings().ai

            assertEquals("sk-legacy-ai", ai.aiApiKey)
            assertEquals("sk-legacy-tts", ai.ttsApiKey)
            assertEquals("sk-legacy-ai", harness.secureStorage.snapshot()[SecureStorageKey.AiApiKey.value])
            assertEquals("sk-legacy-tts", harness.secureStorage.snapshot()[SecureStorageKey.TtsApiKey.value])
            assertTrue("ai_api_key" !in harness.aiCategoryRowKeys())
            assertTrue("tts_api_key" !in harness.aiCategoryRowKeys())
        }

    @Test
    fun `legacy plaintext api key migration retries after a secure storage write failure`() =
        runTest {
            val harness = Harness()
            harness.upsertLegacyAiEntries(
                "ai_api_key" to "sk-legacy-ai",
                "ai_provider" to AiProvider.OpenAi.id,
            )
            harness.secureStorage.failWrites = true

            assertNull(
                harness.repository
                    .readSettings()
                    .ai.aiApiKey,
            )
            assertTrue("ai_api_key" in harness.aiCategoryRowKeys(), "plaintext row remains for a retry")

            harness.secureStorage.failWrites = false
            val ai = harness.repository.readSettings().ai

            assertEquals("sk-legacy-ai", ai.aiApiKey)
            assertEquals("sk-legacy-ai", harness.secureStorage.snapshot()[SecureStorageKey.AiApiKey.value])
            assertTrue("ai_api_key" !in harness.aiCategoryRowKeys())
        }

    @Test
    fun `legacy OpenAI TTS key different from AI key is inferred as separate after migration`() =
        runTest {
            val harness = Harness()
            harness.upsertLegacyAiEntries(
                "ai_api_key" to "sk-ai",
                "ai_provider" to AiProvider.OpenAi.id,
                "tts_api_key" to "sk-tts-separate",
                "tts_provider" to TtsProvider.OpenAi.id,
            )

            val ai = harness.repository.readSettings().ai

            assertTrue(ai.ttsSeparateKey)
            assertEquals("sk-tts-separate", ai.ttsApiKey)
        }

    @Test
    fun `clearing the AI api key deletes it from secure storage`() =
        runTest {
            val harness = Harness()
            harness.repository.updateAiSettings { it.copy(aiApiKey = "sk-initial") }

            harness.repository.updateAiSettings { it.copy(aiApiKey = null) }

            assertNull(harness.secureStorage.snapshot()[SecureStorageKey.AiApiKey.value])
            assertNull(
                harness.repository
                    .readSettings()
                    .ai.aiApiKey,
            )
        }
}
