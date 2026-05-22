package app.sensee.feature.profile.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import kotlinx.coroutines.flow.StateFlow

public interface ProfileAiSettingsComponent : AppComponent {
    public val uiState: StateFlow<ProfileAiSettingsUiState>

    public fun onAction(action: ProfileAiSettingsAction)

    public fun interface Factory {
        public fun create(componentContext: AppComponentContext): ProfileAiSettingsComponent
    }
}
