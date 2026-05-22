package app.sensee.feature.profile.presentation.navigationApi

import app.sensee.core.decompose.navigation.ScreenConfig
import kotlinx.serialization.Serializable

@Serializable
public sealed interface ProfileConfig : ScreenConfig {
    /** Profile section landing — the settings-category menu. */
    @Serializable
    public object Home : ProfileConfig

    @Serializable
    public object AppSettings : ProfileConfig

    @Serializable
    public object LearningSettings : ProfileConfig

    @Serializable
    public object PracticeSettings : ProfileConfig

    @Serializable
    public object AiSettings : ProfileConfig

    @Serializable
    public object ExperimentalSettings : ProfileConfig
}
