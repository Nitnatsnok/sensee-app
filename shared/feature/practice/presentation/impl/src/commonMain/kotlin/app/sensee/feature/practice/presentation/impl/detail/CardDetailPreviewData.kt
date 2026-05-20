package app.sensee.feature.practice.presentation.impl.detail

import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.practice.presentation.api.CardDetailCardUiState
import app.sensee.feature.practice.presentation.api.CardDetailUiState
import app.sensee.feature.practice.presentation.api.RelatedCardUiState
import app.sensee.grammar.domain.GrammarUnitType
import kotlinx.collections.immutable.persistentListOf

internal object CardDetailPreviewData {
    private val card =
        CardDetailCardUiState(
            id = "card-run",
            lemmaId = "lemma-run",
            headword = "run",
            translation = "бежать",
            contextSentence = "She had to run to catch the last train.",
            unitType = GrammarUnitType.IrregularVerb,
            senseSummary = "move at a speed faster than a walk",
            explanation = "Irregular verb: run / ran / run.",
        )

    private val related =
        persistentListOf(
            RelatedCardUiState(
                id = "card-run",
                headword = "run",
                unitType = GrammarUnitType.IrregularVerb,
                translation = "бежать",
                senseSummary = "move fast on foot",
                isCurrent = true,
            ),
            RelatedCardUiState(
                id = "card-run-out",
                headword = "run out",
                unitType = GrammarUnitType.PhrasalVerb,
                translation = "заканчиваться",
                senseSummary = "have no more of something",
                isCurrent = false,
            ),
        )

    val loading = CardDetailUiState(loadingState = DataLoadingState.Loading)

    val error =
        CardDetailUiState(loadingState = DataLoadingState.Error(IllegalStateException("Card not found")))

    val empty = CardDetailUiState(loadingState = DataLoadingState.Success, card = null)

    val content =
        CardDetailUiState(
            loadingState = DataLoadingState.Success,
            card = card,
            lemmaText = "run",
        )

    val contentWithRelated =
        CardDetailUiState(
            loadingState = DataLoadingState.Success,
            card = card,
            lemmaText = "run",
            relatedCards = related,
        )
}
