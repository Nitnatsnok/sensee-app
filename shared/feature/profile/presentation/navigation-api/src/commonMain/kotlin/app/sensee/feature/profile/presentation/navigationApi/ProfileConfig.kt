package app.sensee.feature.profile.presentation.navigationApi

import app.sensee.core.decompose.navigation.ScreenConfig
import kotlinx.serialization.Serializable

@Serializable
public sealed interface ProfileConfig : ScreenConfig {
    /** Profile section landing — the settings-category menu. */
    @Serializable
    public object Home : ProfileConfig

    /** Detail-pane targets exposed in the Profile settings menu. */
    @Serializable
    public sealed interface Settings : ProfileConfig {
        @Serializable
        public object App : Settings

        @Serializable
        public object Learning : Settings

        @Serializable
        public object Practice : Settings

        @Serializable
        public object Ai : Settings

        @Serializable
        public object Experimental : Settings
    }
}
