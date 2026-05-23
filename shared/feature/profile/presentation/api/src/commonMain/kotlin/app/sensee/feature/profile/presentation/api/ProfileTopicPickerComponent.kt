package app.sensee.feature.profile.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import kotlinx.coroutines.flow.StateFlow

/**
 * Topic-preferences picker. Owns its own catalog/selection state and persists
 * toggles through the settings repository — the host decides where to render
 * it (third pane on wide layouts, modal sheet on narrower ones) and supplies
 * the `onClose` callback that fires from [ProfileTopicPickerAction.Close].
 */
public interface ProfileTopicPickerComponent : AppComponent {
    public val uiState: StateFlow<ProfileTopicPickerUiState>

    public fun onAction(action: ProfileTopicPickerAction)

    public fun interface Factory {
        public fun create(
            componentContext: AppComponentContext,
            onClose: () -> Unit,
        ): ProfileTopicPickerComponent
    }
}
