package app.sensee.feature.profile.presentation.impl.appsettings

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.logic.LogicKey
import app.sensee.core.decompose.logic.getOrCreateLogic
import app.sensee.feature.profile.presentation.api.ProfileAppSettingsAction
import app.sensee.feature.profile.presentation.api.ProfileAppSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileAppSettingsUiState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
public class DefaultProfileAppSettingsComponent(
    @Assisted componentContext: AppComponentContext,
    private val profileAppSettingsLogicFactory: ProfileAppSettingsLogic.Factory,
) : ProfileAppSettingsComponent,
    AppComponentContext by componentContext {
    private val logic =
        getOrCreateLogic(LogicKey("ProfileAppSettingsLogic")) {
            profileAppSettingsLogicFactory.create()
        }

    override val uiState: StateFlow<ProfileAppSettingsUiState> = logic.uiState

    override fun onAction(action: ProfileAppSettingsAction) {
        when (action) {
            is ProfileAppSettingsAction.SetThemeMode -> logic.setThemeMode(action.themeMode)
            is ProfileAppSettingsAction.SetHapticFeedbackEnabled ->
                logic.setHapticFeedbackEnabled(action.enabled)
        }
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<ProfileAppSettingsComponent.Factory>(),
    )
    public fun interface Factory : ProfileAppSettingsComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultProfileAppSettingsComponent
    }
}
