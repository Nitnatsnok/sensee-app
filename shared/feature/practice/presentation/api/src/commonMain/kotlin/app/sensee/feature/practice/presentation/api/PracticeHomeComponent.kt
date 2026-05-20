package app.sensee.feature.practice.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.presentation.DataLoadingState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow

public interface PracticeHomeComponent : AppComponent {
    public val uiState: StateFlow<PracticeHomeUiState>

    public fun onAction(action: PracticeHomeAction)

    public fun interface Factory {
        public fun create(componentContext: AppComponentContext): PracticeHomeComponent
    }
}

public data class PracticeHomeUiState(
    val loadingState: DataLoadingState = DataLoadingState.Loading,
    val decks: PersistentList<DeckSummaryUiState> = persistentListOf(),
)

public data class DeckSummaryUiState(
    val id: String,
    val title: String,
    val description: String,
    val cardCount: Int,
)

public sealed interface PracticeHomeAction {
    public data object Retry : PracticeHomeAction

    public data class OpenDeck(
        val deckId: String,
    ) : PracticeHomeAction
}
