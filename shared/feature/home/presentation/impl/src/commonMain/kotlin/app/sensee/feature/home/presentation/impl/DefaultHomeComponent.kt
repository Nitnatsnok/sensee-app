package app.sensee.feature.home.presentation.impl

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.logic.LogicKey
import app.sensee.core.decompose.logic.getOrCreateLogic
import app.sensee.feature.home.presentation.api.HomeAction
import app.sensee.feature.home.presentation.api.HomeComponent
import app.sensee.feature.home.presentation.api.HomeUiState
import app.sensee.feature.practice.presentation.navigationApi.PracticeConfig
import com.arkivanov.essenty.lifecycle.doOnResume
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
public class DefaultHomeComponent(
    @Assisted componentContext: AppComponentContext,
    private val homeLogicFactory: HomeLogic.Factory,
) : HomeComponent,
    AppComponentContext by componentContext {
    private val logic =
        getOrCreateLogic(LogicKey("HomeLogic")) {
            homeLogicFactory.create()
        }

    init {
        // The single trigger for the aggregates: subscribe on Home's first appearance and
        // re-read "due as of now" on every later return to the foreground (HomeLogic does not
        // subscribe eagerly in init), so cards that fell due while away are reflected without an
        // app restart — and an aggregate read happens once per appearance, not twice.
        lifecycle.doOnResume { logic.refresh() }
    }

    override val uiState: StateFlow<HomeUiState> = logic.uiState

    override fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Retry -> logic.refresh()
            HomeAction.StartDueSession -> navigation.open(PracticeConfig.DuePractice)
        }
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<HomeComponent.Factory>(),
    )
    public fun interface Factory : HomeComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultHomeComponent
    }
}
