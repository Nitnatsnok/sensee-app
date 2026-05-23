package app.sensee.feature.profile.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import kotlinx.coroutines.flow.StateFlow

public interface ProfileLearningSettingsComponent : AppComponent {
    public val uiState: StateFlow<ProfileLearningSettingsUiState>

    public fun onAction(action: ProfileLearningSettingsAction)

    public fun interface Factory {
        public fun create(componentContext: AppComponentContext): ProfileLearningSettingsComponent
    }
}
