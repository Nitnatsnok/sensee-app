package app.sensee.feature.profile.presentation.api

public sealed interface ProfileLearningSettingsAction {
    /** The picker is rendered by the host section and persists changes directly. */
    public data object OpenPicker : ProfileLearningSettingsAction

    public data object Retry : ProfileLearningSettingsAction
}
