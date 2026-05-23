package app.sensee.feature.profile.presentation.impl.learningsettings

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.logic.LogicKey
import app.sensee.core.decompose.logic.getOrCreateLogic
import app.sensee.feature.profile.presentation.api.ProfileLearningSettingsAction
import app.sensee.feature.profile.presentation.api.ProfileLearningSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileLearningSettingsUiState
import app.sensee.feature.profile.presentation.navigationApi.ProfileExtraConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
public class DefaultProfileLearningSettingsComponent(
    @Assisted componentContext: AppComponentContext,
    private val profileLearningSettingsLogicFactory: ProfileLearningSettingsLogic.Factory,
) : ProfileLearningSettingsComponent,
    AppComponentContext by componentContext {
    private val logic =
        getOrCreateLogic(LogicKey("ProfileLearningSettingsLogic")) {
            profileLearningSettingsLogicFactory.create()
        }

    override val uiState: StateFlow<ProfileLearningSettingsUiState> = logic.uiState

    override fun onAction(action: ProfileLearningSettingsAction) {
        when (action) {
            ProfileLearningSettingsAction.OpenPicker -> navigation.open(ProfileExtraConfig.TopicPicker)
            ProfileLearningSettingsAction.Retry -> logic.load()
        }
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<ProfileLearningSettingsComponent.Factory>(),
    )
    public fun interface Factory : ProfileLearningSettingsComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultProfileLearningSettingsComponent
    }
}
