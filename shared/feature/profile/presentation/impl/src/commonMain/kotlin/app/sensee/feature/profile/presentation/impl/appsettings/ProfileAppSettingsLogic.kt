package app.sensee.feature.profile.presentation.impl.appsettings

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.decompose.logic.BaseLogic
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.feature.profile.presentation.api.ProfileAppSettingsUiState
import app.sensee.settings.domain.AppThemeMode
import app.sensee.settings.domain.UserSettingsRepository
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AssistedInject
public class ProfileAppSettingsLogic(
    private val settingsRepository: UserSettingsRepository,
    appDispatchers: AppDispatchers,
    appDiagnostics: AppDiagnostics,
) : BaseLogic(appDispatchers, appDiagnostics) {
    @AssistedFactory
    public fun interface Factory {
        public fun create(): ProfileAppSettingsLogic
    }

    private val mutableUiState = MutableStateFlow(ProfileAppSettingsUiState())
    private var lastPersistedThemeMode = AppThemeMode.System
    private var saveGeneration = 0

    public val uiState: StateFlow<ProfileAppSettingsUiState> = mutableUiState.asStateFlow()

    init {
        logicScope.launch {
            settingsRepository.observeSettings().collectLatest { snapshot ->
                lastPersistedThemeMode = snapshot.app.themeMode
                mutableUiState.update { it.copy(themeMode = snapshot.app.themeMode) }
            }
        }
    }

    public fun setThemeMode(themeMode: AppThemeMode) {
        if (themeMode == mutableUiState.value.themeMode) {
            return
        }
        val generation = ++saveGeneration
        mutableUiState.update { it.copy(themeMode = themeMode) }
        logicScope.launch {
            runCatchingCancellable {
                settingsRepository.updateAppSettings { app -> app.copy(themeMode = themeMode) }
            }.onSuccess { snapshot ->
                lastPersistedThemeMode = snapshot.app.themeMode
                if (generation == saveGeneration) {
                    mutableUiState.update { it.copy(themeMode = snapshot.app.themeMode) }
                }
            }.onFailure { throwable ->
                logger.error(throwable) { "Failed to persist app theme mode" }
                if (generation == saveGeneration) {
                    mutableUiState.update { it.copy(themeMode = readPersistedThemeMode()) }
                }
            }
        }
    }

    private suspend fun readPersistedThemeMode(): AppThemeMode =
        runCatchingCancellable {
            settingsRepository.readSettings().app.themeMode
        }.getOrElse { throwable ->
            logger.error(throwable) { "Failed to reload app theme mode after a persist failure" }
            lastPersistedThemeMode
        }.also { themeMode -> lastPersistedThemeMode = themeMode }
}
