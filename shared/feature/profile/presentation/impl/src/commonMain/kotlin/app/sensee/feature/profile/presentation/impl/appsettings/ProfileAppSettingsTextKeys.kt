package app.sensee.feature.profile.presentation.impl.appsettings

import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.TextKey
import app.sensee.settings.domain.AppThemeMode

internal object ProfileAppSettingsTextKeys {
    val SectionAppearance = TextKey("profile.appSettings.section.appearance")
    val ThemeMode = TextKey("profile.appSettings.theme_mode")
    val ThemeModeSystem = TextKey("profile.appSettings.theme_mode.system")
    val ThemeModeLight = TextKey("profile.appSettings.theme_mode.light")
    val ThemeModeDark = TextKey("profile.appSettings.theme_mode.dark")
}

internal fun AppThemeMode.labelKey(): TextKey =
    when (this) {
        AppThemeMode.System -> ProfileAppSettingsTextKeys.ThemeModeSystem
        AppThemeMode.Light -> ProfileAppSettingsTextKeys.ThemeModeLight
        AppThemeMode.Dark -> ProfileAppSettingsTextKeys.ThemeModeDark
    }

internal val DefaultProfileAppSettingsTextProvider =
    MapTextProvider(
        mapOf(
            ProfileAppSettingsTextKeys.SectionAppearance to "Внешний вид",
            ProfileAppSettingsTextKeys.ThemeMode to "Тема",
            ProfileAppSettingsTextKeys.ThemeModeSystem to "Системная",
            ProfileAppSettingsTextKeys.ThemeModeLight to "Светлая",
            ProfileAppSettingsTextKeys.ThemeModeDark to "Тёмная",
        ),
    )
