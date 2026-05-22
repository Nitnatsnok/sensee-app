package app.sensee.feature.profile.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig

/**
 * Detail-panel host for a settings category that has no screen yet. Carries
 * only its [config] so the screen can render an honest "coming later" state.
 */
public interface ProfileSettingsPlaceholderComponent : AppComponent {
    public val config: ProfileConfig
}
