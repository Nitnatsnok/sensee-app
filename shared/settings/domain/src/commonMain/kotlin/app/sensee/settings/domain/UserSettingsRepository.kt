package app.sensee.settings.domain

import kotlinx.coroutines.flow.Flow

public interface UserSettingsRepository {
    public fun observeSettings(scope: UserSettingsScope = UserSettingsScope.Device): Flow<UserSettingsSnapshot>

    public suspend fun readSettings(scope: UserSettingsScope = UserSettingsScope.Device): UserSettingsSnapshot

    public suspend fun updateSettings(
        scope: UserSettingsScope = UserSettingsScope.Device,
        transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
    ): UserSettingsSnapshot

    public suspend fun updateAppSettings(
        scope: UserSettingsScope = UserSettingsScope.Device,
        transform: (AppSettings) -> AppSettings,
    ): UserSettingsSnapshot = updateSettings(scope) { settings -> settings.copy(app = transform(settings.app)) }

    public suspend fun updateLearningSettings(
        scope: UserSettingsScope = UserSettingsScope.Device,
        transform: (LearningSettings) -> LearningSettings,
    ): UserSettingsSnapshot =
        updateSettings(scope) { settings -> settings.copy(learning = transform(settings.learning)) }

    public suspend fun updatePracticeSettings(
        scope: UserSettingsScope = UserSettingsScope.Device,
        transform: (PracticeSettings) -> PracticeSettings,
    ): UserSettingsSnapshot =
        updateSettings(scope) { settings -> settings.copy(practice = transform(settings.practice)) }

    public suspend fun updateAiSettings(
        scope: UserSettingsScope = UserSettingsScope.Device,
        transform: (AiSettings) -> AiSettings,
    ): UserSettingsSnapshot = updateSettings(scope) { settings -> settings.copy(ai = transform(settings.ai)) }
}
