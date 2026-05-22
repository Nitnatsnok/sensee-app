package app.sensee.feature.profile.presentation.impl.aisettings

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.logic.LogicKey
import app.sensee.core.decompose.logic.getOrCreateLogic
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsAction
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsUiState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
public class DefaultProfileAiSettingsComponent(
    @Assisted componentContext: AppComponentContext,
    private val profileAiSettingsLogicFactory: ProfileAiSettingsLogic.Factory,
) : ProfileAiSettingsComponent,
    AppComponentContext by componentContext {
    private val logic =
        getOrCreateLogic(LogicKey("ProfileAiSettingsLogic")) {
            profileAiSettingsLogicFactory.create()
        }

    override val uiState: StateFlow<ProfileAiSettingsUiState> = logic.uiState

    override fun onAction(action: ProfileAiSettingsAction): Unit = logic.onAction(action)

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<ProfileAiSettingsComponent.Factory>(),
    )
    public fun interface Factory : ProfileAiSettingsComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultProfileAiSettingsComponent
    }
}
