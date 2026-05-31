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

    public val uiState: StateFlow<ProfileAppSettingsUiState> = mutableUiState.asStateFlow()

    init {
        logicScope.launch {
            settingsRepository.observeSettings().collect { snapshot ->
                mutableUiState.update {
                    it.copy(
                        themeMode = snapshot.app.themeMode,
                        hapticFeedbackEnabled = snapshot.app.hapticFeedbackEnabled,
                    )
                }
            }
        }
    }

    public fun setThemeMode(themeMode: AppThemeMode) {
        if (themeMode == mutableUiState.value.themeMode) {
            return
        }
        logicScope.launch {
            runCatchingCancellable {
                settingsRepository.updateAppSettings { app -> app.copy(themeMode = themeMode) }
            }.onFailure { throwable ->
                logger.error(throwable) { "Failed to persist app theme mode" }
            }
        }
    }

    public fun setHapticFeedbackEnabled(enabled: Boolean) {
        if (enabled == mutableUiState.value.hapticFeedbackEnabled) {
            return
        }
        logicScope.launch {
            runCatchingCancellable {
                settingsRepository.updateAppSettings { app -> app.copy(hapticFeedbackEnabled = enabled) }
            }.onFailure { throwable ->
                logger.error(throwable) { "Failed to persist haptic feedback setting" }
            }
        }
    }
}
