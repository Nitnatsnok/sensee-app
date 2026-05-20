package app.sensee.settings.domain

public data class UserSettingsSnapshot(
    val app: AppSettings = AppSettings(),
    val learning: LearningSettings = LearningSettings(),
    val practice: PracticeSettings = PracticeSettings(),
    val ai: AiSettings = AiSettings(),
)
