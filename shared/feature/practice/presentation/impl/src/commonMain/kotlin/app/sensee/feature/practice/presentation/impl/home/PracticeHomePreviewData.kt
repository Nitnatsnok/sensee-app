package app.sensee.feature.practice.presentation.impl.home

import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.practice.presentation.api.DeckSummaryUiState
import app.sensee.feature.practice.presentation.api.PracticeHomeUiState
import kotlinx.collections.immutable.persistentListOf

internal object PracticeHomePreviewData {
    private val decks =
        persistentListOf(
            DeckSummaryUiState(
                id = "deck-irregular",
                title = "Irregular verbs",
                description = "The 100 most common irregular verbs.",
                cardCount = 100,
            ),
            DeckSummaryUiState(
                id = "deck-phrasal",
                title = "Phrasal verbs",
                description = "Everyday phrasal verbs grouped by particle.",
                cardCount = 64,
            ),
        )

    val loading = PracticeHomeUiState(loadingState = DataLoadingState.Loading)

    val error =
        PracticeHomeUiState(loadingState = DataLoadingState.Error(IllegalStateException("Network unavailable")))

    val empty = PracticeHomeUiState(loadingState = DataLoadingState.Success)

    val content =
        PracticeHomeUiState(
            loadingState = DataLoadingState.Success,
            decks = decks,
        )
}
