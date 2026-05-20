package app.sensee.feature.practice.presentation.impl.detail

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.logic.LogicKey
import app.sensee.core.decompose.logic.getOrCreateLogic
import app.sensee.feature.practice.presentation.api.CardDetailAction
import app.sensee.feature.practice.presentation.api.CardDetailComponent
import app.sensee.feature.practice.presentation.api.CardDetailUiState
import app.sensee.feature.practice.presentation.navigationApi.PracticeConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
public class DefaultCardDetailComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val args: CardDetailComponent.Args,
    private val cardDetailLogicFactory: CardDetailLogic.Factory,
) : CardDetailComponent,
    AppComponentContext by componentContext {
    private val logic =
        getOrCreateLogic(LogicKey("CardDetailLogic:${args.cardId}")) {
            cardDetailLogicFactory.create(cardId = args.cardId)
        }

    override val uiState: StateFlow<CardDetailUiState> = logic.uiState

    override fun onAction(action: CardDetailAction) {
        when (action) {
            CardDetailAction.Retry -> logic.load()
            is CardDetailAction.OpenRelated ->
                navigation.open(PracticeConfig.CardDetail(action.cardId))
        }
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<CardDetailComponent.Factory>(),
    )
    public fun interface Factory : CardDetailComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            args: CardDetailComponent.Args,
        ): DefaultCardDetailComponent
    }
}
