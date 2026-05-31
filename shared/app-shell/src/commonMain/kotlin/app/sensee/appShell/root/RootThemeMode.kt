package app.sensee.appShell.root

import app.sensee.settings.domain.AppThemeMode
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.ui.designSystem.theme.SenseeThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun UserSettingsRepository.observeThemeMode(): Flow<SenseeThemeMode> =
    observeSettings()
        .map { snapshot -> snapshot.app.themeMode.toSenseeThemeMode() }
        .distinctUntilChanged()

internal fun UserSettingsRepository.observeHapticFeedbackEnabled(): Flow<Boolean> =
    observeSettings()
        .map { snapshot -> snapshot.app.hapticFeedbackEnabled }
        .distinctUntilChanged()

private fun AppThemeMode.toSenseeThemeMode(): SenseeThemeMode =
    when (this) {
        AppThemeMode.System -> SenseeThemeMode.System
        AppThemeMode.Light -> SenseeThemeMode.Light
        AppThemeMode.Dark -> SenseeThemeMode.Dark
    }
