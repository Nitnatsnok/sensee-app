package app.sensee.settings.domain

public enum class UserSettingsCategory(
    public val storageName: String,
) {
    App("app"),
    Learning("learning"),
    Practice("practice"),
    Ai("ai"),
    Experimental("experimental"),
}
