package app.sensee.feature.library.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.presentation.DataLoadingState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow

public interface LibraryHomeComponent : AppComponent {
    public val uiState: StateFlow<LibraryHomeUiState>

    public fun onAction(action: LibraryHomeAction)

    public fun interface Factory {
        public fun create(componentContext: AppComponentContext): LibraryHomeComponent
    }
}

public data class LibraryHomeUiState(
    val loadingState: DataLoadingState = DataLoadingState.Loading,
    val suggested: PersistentList<LibraryDeckUiState> = persistentListOf(),
    val owned: PersistentList<LibraryDeckUiState> = persistentListOf(),
)

public data class LibraryDeckUiState(
    val id: String,
    val title: String,
    val description: String,
    val cardCount: Int,
    /** False for the derived captured deck — owned but not detachable. */
    val canUnAdopt: Boolean,
)

public sealed interface LibraryHomeAction {
    public data object Retry : LibraryHomeAction

    public data class Adopt(
        val deckId: String,
    ) : LibraryHomeAction

    public data class UnAdopt(
        val deckId: String,
    ) : LibraryHomeAction
}
