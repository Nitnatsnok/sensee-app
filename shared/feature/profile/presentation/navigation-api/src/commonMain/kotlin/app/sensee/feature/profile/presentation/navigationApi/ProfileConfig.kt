package app.sensee.feature.profile.presentation.navigationApi

import app.sensee.core.decompose.navigation.ScreenConfig
import kotlinx.serialization.Serializable

@Serializable
public sealed interface ProfileConfig : ScreenConfig {
    @Serializable
    public object Home : ProfileConfig
}
