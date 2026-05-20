package app.sensee.feature.profile.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import kotlinx.coroutines.flow.StateFlow

public interface ProfileHomeComponent : AppComponent {
    public val uiState: StateFlow<ProfileHomeUiState>

    public fun onAction(action: ProfileHomeAction)

    public fun interface Factory {
        public fun create(componentContext: AppComponentContext): ProfileHomeComponent
    }
}
