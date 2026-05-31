package app.sensee.settings.data

import app.sensee.core.database.User_setting
import app.sensee.core.secureStorage.SecureStorage
import app.sensee.core.secureStorage.SecureStorageKey
import app.sensee.settings.domain.AiProvider
import app.sensee.settings.domain.AiSettings
import app.sensee.settings.domain.AppSettings
import app.sensee.settings.domain.AppThemeMode
import app.sensee.settings.domain.LearningSettings
import app.sensee.settings.domain.PracticeSettings
import app.sensee.settings.domain.TtsProvider
import app.sensee.settings.domain.UserSettingsCategory
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<UserSettingsRepository>(),
)
@Inject
public class DefaultUserSettingsRepository(
    private val localDataSource: UserSettingsLocalDataSource,
    private val secureStorage: SecureStorage,
    json: Json,
) : UserSettingsRepository {
    private val codec = SettingsRowCodec(json)
    private val updateMutex = Mutex()
    private val legacyMigration = LegacyAiSecretsMigration(localDataSource, secureStorage, json)

    override fun observeSettings(scope: UserSettingsScope): Flow<UserSettingsSnapshot> =
        localDataSource
            .observeRows(scope)
            .map { rows ->
                legacyMigration.ensureMigrated(scope, rows)
                toSnapshot(rows, readSecrets())
            }

    override suspend fun readSettings(scope: UserSettingsScope): UserSettingsSnapshot {
        val rows = localDataSource.selectRows(scope)
        legacyMigration.ensureMigrated(scope, rows)
        return toSnapshot(rows, readSecrets())
    }

    override suspend fun updateSettings(
        scope: UserSettingsScope,
        transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
    ): UserSettingsSnapshot =
        updateMutex.withLock {
            val rows = localDataSource.selectRows(scope)
            legacyMigration.ensureMigrated(scope, rows)
            val current = toSnapshot(rows, readSecrets())
            val next = transform(current)
            writeSecretsIfChanged(current.ai, next.ai)
            localDataSource.upsertEntries(scope, next.toEntries())
            next
        }

    private suspend fun readSecrets(): AiSecrets =
        AiSecrets(
            aiApiKey = readSecret(SecureStorageKey.AiApiKey),
            ttsApiKey = readSecret(SecureStorageKey.TtsApiKey),
        )

    // A broken vault on read degrades to "not configured" so the UI still
    // opens. Writes still surface their errors (see [writeSecretsIfChanged]),
    // so a user attempting to save a new key learns immediately if the vault
    // is unhealthy.
    private suspend fun readSecret(key: SecureStorageKey): String? =
        try {
            secureStorage.read(key)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            null
        }

    private suspend fun writeSecretsIfChanged(
        previous: AiSettings,
        next: AiSettings,
    ) {
        if (previous.aiApiKey != next.aiApiKey) {
            secureStorage.write(SecureStorageKey.AiApiKey, next.aiApiKey?.takeIf { it.isNotBlank() })
        }
        if (previous.ttsApiKey != next.ttsApiKey) {
            secureStorage.write(SecureStorageKey.TtsApiKey, next.ttsApiKey?.takeIf { it.isNotBlank() })
        }
    }

    private fun toSnapshot(
        rows: List<User_setting>,
        secrets: AiSecrets,
    ): UserSettingsSnapshot {
        val values = rows.associateBy { UserSettingId(category = it.category, key = it.key) }
        val aiProvider = AiProvider.fromId(codec.stringValue(values, AiKeys.AiProvider, null))
        val ttsProvider = TtsProvider.fromId(codec.stringValue(values, AiKeys.TtsProvider, null))
        val aiApiKey = secrets.aiApiKey?.takeIf { it.isNotBlank() }
        val ttsApiKey = secrets.ttsApiKey?.takeIf { it.isNotBlank() }
        return UserSettingsSnapshot(
            app =
                AppSettings(
                    themeMode = codec.enumValue(values, AppKeys.ThemeMode, AppThemeMode.System),
                    interfaceLanguageTag = codec.stringValue(values, AppKeys.InterfaceLanguageTag, null),
                    hapticFeedbackEnabled = codec.booleanValue(values, AppKeys.HapticFeedbackEnabled, true),
                ),
            learning =
                LearningSettings(
                    studyLanguageTag = codec.stringValue(values, LearningKeys.StudyLanguageTag, null),
                    translationLanguageTag = codec.stringValue(values, LearningKeys.TranslationLanguageTag, null),
                    preferredTopicIds =
                        codec.stringListValue(values, LearningKeys.PreferredTopicIds, emptyList()).toSet(),
                ),
            practice =
                PracticeSettings(
                    autoPlayAudio = codec.booleanValue(values, PracticeKeys.AutoPlayAudio, false),
                    dailyGoal =
                        codec.intValue(
                            values,
                            PracticeKeys.DailyGoal,
                            PracticeSettings.DEFAULT_DAILY_GOAL,
                        ),
                    showTranscription = codec.booleanValue(values, PracticeKeys.ShowTranscription, true),
                ),
            ai = aiSettings(values, aiProvider, ttsProvider, aiApiKey, ttsApiKey),
        )
    }

    private fun aiSettings(
        values: Map<UserSettingId, User_setting>,
        aiProvider: AiProvider,
        ttsProvider: TtsProvider,
        aiApiKey: String?,
        ttsApiKey: String?,
    ): AiSettings =
        AiSettings(
            aiApiKey = aiApiKey,
            aiProvider = aiProvider,
            aiModel = codec.stringValue(values, AiKeys.AiModel, null),
            ttsApiKey = ttsApiKey,
            ttsProvider = ttsProvider,
            ttsModel = codec.stringValue(values, AiKeys.TtsModel, null),
            ttsVoiceId = codec.stringValue(values, AiKeys.TtsVoiceId, null),
            ttsSeparateKey =
                codec.booleanValue(
                    values,
                    AiKeys.TtsSeparateKey,
                    inferTtsSeparateKey(
                        aiProvider = aiProvider,
                        ttsProvider = ttsProvider,
                        aiApiKey = aiApiKey,
                        ttsApiKey = ttsApiKey,
                    ),
                ),
        )

    private fun UserSettingsSnapshot.toEntries(): List<UserSettingEntry> =
        listOf(
            codec.entry(AppKeys.ThemeMode, app.themeMode.name),
            codec.entry(AppKeys.InterfaceLanguageTag, app.interfaceLanguageTag),
            codec.entry(AppKeys.HapticFeedbackEnabled, app.hapticFeedbackEnabled),
            codec.entry(LearningKeys.StudyLanguageTag, learning.studyLanguageTag),
            codec.entry(LearningKeys.TranslationLanguageTag, learning.translationLanguageTag),
            codec.entry(LearningKeys.PreferredTopicIds, learning.preferredTopicIds.sorted()),
            codec.entry(PracticeKeys.AutoPlayAudio, practice.autoPlayAudio),
            codec.entry(PracticeKeys.DailyGoal, practice.dailyGoal),
            codec.entry(PracticeKeys.ShowTranscription, practice.showTranscription),
            codec.entry(AiKeys.AiProvider, ai.aiProvider.id),
            codec.entry(AiKeys.AiModel, ai.aiModel),
            codec.entry(AiKeys.TtsProvider, ai.ttsProvider.id),
            codec.entry(AiKeys.TtsModel, ai.ttsModel),
            codec.entry(AiKeys.TtsVoiceId, ai.ttsVoiceId),
            codec.entry(AiKeys.TtsSeparateKey, ai.ttsSeparateKey),
        )
}

internal const val CURRENT_SCHEMA_VERSION = 1L

private data class AiSecrets(
    val aiApiKey: String?,
    val ttsApiKey: String?,
)

private enum class AppKeys(
    override val key: String,
) : SettingKey {
    ThemeMode("theme_mode"),
    InterfaceLanguageTag("interface_language_tag"),
    HapticFeedbackEnabled("haptic_feedback_enabled"),
    ;

    override val category: UserSettingsCategory = UserSettingsCategory.App
}

private enum class LearningKeys(
    override val key: String,
) : SettingKey {
    StudyLanguageTag("study_language_tag"),
    TranslationLanguageTag("translation_language_tag"),
    PreferredTopicIds("preferred_topic_ids"),
    ;

    override val category: UserSettingsCategory = UserSettingsCategory.Learning
}

private enum class PracticeKeys(
    override val key: String,
) : SettingKey {
    AutoPlayAudio("auto_play_audio"),
    DailyGoal("daily_goal"),
    ShowTranscription("show_transcription"),
    ;

    override val category: UserSettingsCategory = UserSettingsCategory.Practice
}

private enum class AiKeys(
    override val key: String,
) : SettingKey {
    AiProvider("ai_provider"),
    AiModel("ai_model"),
    TtsProvider("tts_provider"),
    TtsModel("tts_model"),
    TtsVoiceId("tts_voice_id"),
    TtsSeparateKey("tts_separate_key"),
    ;

    override val category: UserSettingsCategory = UserSettingsCategory.Ai
}

private fun inferTtsSeparateKey(
    aiProvider: AiProvider,
    ttsProvider: TtsProvider,
    aiApiKey: String?,
    ttsApiKey: String?,
): Boolean =
    aiProvider == AiProvider.OpenAi &&
        ttsProvider == TtsProvider.OpenAi &&
        !ttsApiKey.isNullOrBlank() &&
        ttsApiKey.trim() != aiApiKey.orEmpty().trim()
