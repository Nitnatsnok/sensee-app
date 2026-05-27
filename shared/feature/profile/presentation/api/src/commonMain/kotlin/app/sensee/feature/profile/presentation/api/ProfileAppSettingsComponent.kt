package app.sensee.feature.profile.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import kotlinx.coroutines.flow.StateFlow

public interface ProfileAppSettingsComponent : AppComponent {
    public val uiState: StateFlow<ProfileAppSettingsUiState>

    public fun onAction(action: ProfileAppSettingsAction)

    public fun interface Factory {
        public fun create(componentContext: AppComponentContext): ProfileAppSettingsComponent
    }
}
