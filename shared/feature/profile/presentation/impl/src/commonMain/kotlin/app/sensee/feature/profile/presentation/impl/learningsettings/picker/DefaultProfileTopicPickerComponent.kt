package app.sensee.feature.profile.presentation.impl.learningsettings.picker

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.logic.LogicKey
import app.sensee.core.decompose.logic.getOrCreateLogic
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerAction
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerComponent
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerUiState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
public class DefaultProfileTopicPickerComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val onClose: () -> Unit,
    private val logicFactory: ProfileTopicPickerLogic.Factory,
) : ProfileTopicPickerComponent,
    AppComponentContext by componentContext {
    private val logic =
        getOrCreateLogic(LogicKey("ProfileTopicPickerLogic")) {
            logicFactory.create()
        }

    override val uiState: StateFlow<ProfileTopicPickerUiState> = logic.uiState

    override fun onAction(action: ProfileTopicPickerAction) {
        when (action) {
            is ProfileTopicPickerAction.Toggle -> logic.toggleTopic(action.id)
            ProfileTopicPickerAction.Retry -> logic.load()
            ProfileTopicPickerAction.Close -> onClose()
        }
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<ProfileTopicPickerComponent.Factory>(),
    )
    public fun interface Factory : ProfileTopicPickerComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            onClose: () -> Unit,
        ): DefaultProfileTopicPickerComponent
    }
}
