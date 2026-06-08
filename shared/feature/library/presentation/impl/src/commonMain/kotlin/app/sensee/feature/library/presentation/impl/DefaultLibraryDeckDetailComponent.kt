package app.sensee.feature.library.presentation.impl

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.logic.LogicKey
import app.sensee.core.decompose.logic.getOrCreateLogic
import app.sensee.feature.library.presentation.api.LibraryDeckDetailAction
import app.sensee.feature.library.presentation.api.LibraryDeckDetailComponent
import app.sensee.feature.library.presentation.api.LibraryDeckDetailUiState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
public class DefaultLibraryDeckDetailComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val args: LibraryDeckDetailComponent.Args,
    private val libraryDeckDetailLogicFactory: LibraryDeckDetailLogic.Factory,
) : LibraryDeckDetailComponent,
    AppComponentContext by componentContext {
    private val logic =
        getOrCreateLogic(LogicKey("LibraryDeckDetailLogic:${args.deckId}")) {
            libraryDeckDetailLogicFactory.create(deckId = args.deckId)
        }

    override val uiState: StateFlow<LibraryDeckDetailUiState> = logic.uiState

    override fun onAction(action: LibraryDeckDetailAction) {
        when (action) {
            LibraryDeckDetailAction.Retry -> logic.load()
            LibraryDeckDetailAction.Back -> navigation.back()
            LibraryDeckDetailAction.Close -> args.onClose()
            LibraryDeckDetailAction.AdoptDeck -> logic.adopt()
        }
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<LibraryDeckDetailComponent.Factory>(),
    )
    public fun interface Factory : LibraryDeckDetailComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            args: LibraryDeckDetailComponent.Args,
        ): DefaultLibraryDeckDetailComponent
    }
}
