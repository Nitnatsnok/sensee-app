package app.sensee.feature.library.presentation.navigationApi

import app.sensee.core.decompose.navigation.ScreenConfig
import kotlinx.serialization.Serializable

@Serializable
public sealed interface LibraryConfig : ScreenConfig {
    @Serializable
    public object Home : LibraryConfig
}
