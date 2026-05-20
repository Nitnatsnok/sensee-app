package app.sensee.feature.practice.presentation.impl.home

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.logic.LogicKey
import app.sensee.core.decompose.logic.getOrCreateLogic
import app.sensee.feature.practice.presentation.api.PracticeHomeAction
import app.sensee.feature.practice.presentation.api.PracticeHomeComponent
import app.sensee.feature.practice.presentation.api.PracticeHomeUiState
import app.sensee.feature.practice.presentation.navigationApi.PracticeConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
public class DefaultPracticeHomeComponent(
    @Assisted componentContext: AppComponentContext,
    private val practiceHomeLogicFactory: PracticeHomeLogic.Factory,
) : PracticeHomeComponent,
    AppComponentContext by componentContext {
    private val logic =
        getOrCreateLogic(LogicKey("PracticeHomeLogic")) {
            practiceHomeLogicFactory.create()
        }

    override val uiState: StateFlow<PracticeHomeUiState> = logic.uiState

    override fun onAction(action: PracticeHomeAction) {
        when (action) {
            PracticeHomeAction.Retry -> logic.retry()
            is PracticeHomeAction.OpenDeck ->
                navigation.open(PracticeConfig.DeckPractice(deckId = action.deckId))
        }
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<PracticeHomeComponent.Factory>(),
    )
    public fun interface Factory : PracticeHomeComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultPracticeHomeComponent
    }
}
