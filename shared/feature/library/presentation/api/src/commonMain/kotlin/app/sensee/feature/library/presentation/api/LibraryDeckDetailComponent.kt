package app.sensee.feature.library.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.presentation.DataLoadingState
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.lexicon.domain.Sense
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow

public interface LibraryDeckDetailComponent : AppComponent {
    public val uiState: StateFlow<LibraryDeckDetailUiState>

    public fun onAction(action: LibraryDeckDetailAction)

    public data class Args(
        val deckId: String,
        val onClose: () -> Unit = {},
    )

    public fun interface Factory {
        public fun create(
            componentContext: AppComponentContext,
            args: Args,
        ): LibraryDeckDetailComponent
    }
}

public data class LibraryDeckDetailUiState(
    val loadingState: DataLoadingState = DataLoadingState.Loading,
    val title: String = "",
    val description: String = "",
    val cards: PersistentList<LibraryCardUiState> = persistentListOf(),
    /** Resolved taxonomy labels for the card badges; [GrammarLabels.EMPTY] degrades to raw ids. */
    val grammarLabels: GrammarLabels = GrammarLabels.EMPTY,
    /** Study language tag used to pick the label language for the card badges. */
    val studyLanguageTag: String = "en",
    /** A Service suggestion shown before adoption: the screen offers an explicit "add" action. */
    val isService: Boolean = false,
    /** True while the explicit adopt action is in flight, to disable the button. */
    val adopting: Boolean = false,
    /** Non-fatal adopt failure; the already loaded preview stays visible. */
    val adoptError: Throwable? = null,
)

/** One deck card carried whole as a [Sense] so the shared sense card renders its full detail. */
public data class LibraryCardUiState(
    val id: String,
    val sense: Sense,
)

public sealed interface LibraryDeckDetailAction {
    public data object Retry : LibraryDeckDetailAction

    public data object Back : LibraryDeckDetailAction

    public data object Close : LibraryDeckDetailAction

    public data object AdoptDeck : LibraryDeckDetailAction
}
