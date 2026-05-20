package app.sensee.settings.domain

public data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.System,
    val interfaceLanguageTag: String? = null,
)

public enum class AppThemeMode {
    System,
    Light,
    Dark,
}
