package app.sensee.feature.profile.presentation.api

public sealed interface ProfileTopicPickerAction {
    /** Toggle the topic with [id]; persists optimistically. */
    public data class Toggle(
        val id: String,
    ) : ProfileTopicPickerAction

    public data object Retry : ProfileTopicPickerAction

    public data object Close : ProfileTopicPickerAction
}
