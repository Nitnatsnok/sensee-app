package app.sensee.feature.library.presentation.impl

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.logic.LogicKey
import app.sensee.core.decompose.logic.getOrCreateLogic
import app.sensee.feature.library.presentation.api.LibraryHomeAction
import app.sensee.feature.library.presentation.api.LibraryHomeComponent
import app.sensee.feature.library.presentation.api.LibraryHomeUiState
import app.sensee.feature.library.presentation.navigationApi.LibraryConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
public class DefaultLibraryHomeComponent(
    @Assisted componentContext: AppComponentContext,
    private val libraryHomeLogicFactory: LibraryHomeLogic.Factory,
) : LibraryHomeComponent,
    AppComponentContext by componentContext {
    private val logic =
        getOrCreateLogic(LogicKey("LibraryHomeLogic")) {
            libraryHomeLogicFactory.create()
        }

    override val uiState: StateFlow<LibraryHomeUiState> = logic.uiState

    override fun onAction(action: LibraryHomeAction) {
        when (action) {
            LibraryHomeAction.Retry -> logic.retry()
            is LibraryHomeAction.Adopt -> logic.adopt(action.deckId)
            is LibraryHomeAction.UnAdopt -> logic.unAdopt(action.deckId)
            is LibraryHomeAction.OpenDeck -> navigation.open(LibraryConfig.DeckDetail(action.deckId))
        }
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<LibraryHomeComponent.Factory>(),
    )
    public fun interface Factory : LibraryHomeComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultLibraryHomeComponent
    }
}
