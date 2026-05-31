package app.sensee.feature.profile.presentation.api

import app.sensee.settings.domain.AppThemeMode

public data class ProfileAppSettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.System,
    val hapticFeedbackEnabled: Boolean = true,
)
