package app.sensee.settings.domain

public data class PracticeSettings(
    val autoPlayAudio: Boolean = false,
    val dailyGoal: Int = DEFAULT_DAILY_GOAL,
    val showTranscription: Boolean = true,
) {
    public companion object {
        public const val DEFAULT_DAILY_GOAL: Int = 20
    }
}
