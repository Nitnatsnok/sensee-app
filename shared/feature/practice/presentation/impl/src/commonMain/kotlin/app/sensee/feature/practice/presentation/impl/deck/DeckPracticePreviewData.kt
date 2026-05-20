package app.sensee.feature.practice.presentation.impl.deck

import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.practice.domain.PracticeCardFront
import app.sensee.feature.practice.presentation.api.DeckPracticeCardUiState
import app.sensee.feature.practice.presentation.api.DeckPracticeUiState
import app.sensee.grammar.domain.GrammarUnitType
import kotlinx.collections.immutable.persistentListOf

internal object DeckPracticePreviewData {
    private fun card(
        key: String,
        headword: String,
        translation: String,
        context: String,
        front: PracticeCardFront,
    ) = DeckPracticeCardUiState(
        presentationKey = "$key#0",
        id = key,
        lemmaId = "lemma-$key",
        headword = headword,
        translation = translation,
        contextSentence = context,
        unitType = GrammarUnitType.IrregularVerb,
        senseSummary = "move at a speed faster than a walk",
        explanation = "Irregular verb: $headword.",
        practiceFront = front,
    )

    private val cards =
        persistentListOf(
            card(
                key = "card-run",
                headword = "run",
                translation = "бежать",
                context = "She had to run to catch the last train.",
                front = PracticeCardFront.English,
            ),
            card(
                key = "card-take",
                headword = "take",
                translation = "брать",
                context = "Please take a seat.",
                front = PracticeCardFront.Russian,
            ),
        )

    val loading = DeckPracticeUiState(loadingState = DataLoadingState.Loading)

    val error =
        DeckPracticeUiState(loadingState = DataLoadingState.Error(IllegalStateException("Deck not found")))

    val content =
        DeckPracticeUiState(
            loadingState = DataLoadingState.Success,
            deckTitle = "Irregular verbs",
            cards = cards,
        )

    val finished =
        DeckPracticeUiState(
            loadingState = DataLoadingState.Success,
            deckTitle = "Irregular verbs",
            cards = persistentListOf(),
        )
}
