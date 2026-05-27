package app.sensee.feature.profile.presentation.api

import app.sensee.settings.domain.AppThemeMode

public sealed interface ProfileAppSettingsAction {
    public data class SetThemeMode(
        val themeMode: AppThemeMode,
    ) : ProfileAppSettingsAction
}
