package app.sensee.feature.home.presentation.navigationApi

import app.sensee.core.decompose.navigation.ScreenConfig
import kotlinx.serialization.Serializable

@Serializable
public sealed interface HomeConfig : ScreenConfig {
    @Serializable
    public object Home : HomeConfig
}
