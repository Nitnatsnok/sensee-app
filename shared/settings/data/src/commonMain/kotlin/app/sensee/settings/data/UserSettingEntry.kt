package app.sensee.settings.data

import app.sensee.settings.domain.UserSettingsCategory

internal data class UserSettingEntry(
    val category: UserSettingsCategory,
    val key: String,
    val valueJson: String,
)
