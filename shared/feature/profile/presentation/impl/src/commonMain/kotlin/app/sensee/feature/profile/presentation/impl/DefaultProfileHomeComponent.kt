package app.sensee.feature.profile.presentation.impl

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.logic.LogicKey
import app.sensee.core.decompose.logic.getOrCreateLogic
import app.sensee.feature.profile.presentation.api.ProfileHomeAction
import app.sensee.feature.profile.presentation.api.ProfileHomeComponent
import app.sensee.feature.profile.presentation.api.ProfileHomeUiState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
public class DefaultProfileHomeComponent(
    @Assisted componentContext: AppComponentContext,
    private val profileHomeLogicFactory: ProfileHomeLogic.Factory,
) : ProfileHomeComponent,
    AppComponentContext by componentContext {
    private val logic =
        getOrCreateLogic(LogicKey("ProfileHomeLogic")) {
            profileHomeLogicFactory.create()
        }

    override val uiState: StateFlow<ProfileHomeUiState> = logic.uiState

    override fun onAction(action: ProfileHomeAction): Unit = logic.onAction(action)

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<ProfileHomeComponent.Factory>(),
    )
    public fun interface Factory : ProfileHomeComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultProfileHomeComponent
    }
}
