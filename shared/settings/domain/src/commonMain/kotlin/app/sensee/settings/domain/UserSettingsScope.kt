package app.sensee.settings.domain

import kotlin.jvm.JvmInline

@JvmInline
public value class UserSettingsScope(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "UserSettingsScope must not be blank"
        }
    }

    public companion object {
        public val Device: UserSettingsScope = UserSettingsScope("device")
        public val User: UserSettingsScope = UserSettingsScope("user")
    }
}
