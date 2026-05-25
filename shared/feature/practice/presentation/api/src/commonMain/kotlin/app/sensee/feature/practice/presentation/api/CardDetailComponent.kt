package app.sensee.feature.practice.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.presentation.DataLoadingState
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow

public interface CardDetailComponent : AppComponent {
    public val uiState: StateFlow<CardDetailUiState>

    public fun onAction(action: CardDetailAction)

    public data class Args(
        val cardId: String,
    )

    public fun interface Factory {
        public fun create(
            componentContext: AppComponentContext,
            args: Args,
        ): CardDetailComponent
    }
}

public data class CardDetailUiState(
    val loadingState: DataLoadingState = DataLoadingState.Loading,
    val card: CardDetailCardUiState? = null,
    val lemmaText: String = "",
    val relatedCards: PersistentList<RelatedCardUiState> = persistentListOf(),
    val grammarLabels: GrammarLabels = GrammarLabels.EMPTY,
    /** Per-screen lifecycle of the grammar-label dictionary load. */
    val grammarLabelsState: DataLoadingState = DataLoadingState.Idle,
    /** BCP-47 tag of the language the learner is studying — drives short badge labels. */
    val studyLanguageTag: String = "en",
    /** BCP-47 tag of the learner's native language — drives long descriptions. */
    val nativeLanguageTag: String = "ru",
)

public data class CardDetailCardUiState(
    val id: String,
    val lemmaId: String,
    val headword: String,
    val translation: String,
    val contextSentence: String,
    val unitType: GrammarUnitType,
    val grammarTags: PersistentList<GrammarTag> = persistentListOf(),
    val senseSummary: String,
    val explanation: String,
)

public data class RelatedCardUiState(
    val id: String,
    val headword: String,
    val unitType: GrammarUnitType,
    val translation: String,
    val senseSummary: String,
    val isCurrent: Boolean,
)

public sealed interface CardDetailAction {
    public data object Retry : CardDetailAction

    public data object RetryGrammarLabels : CardDetailAction

    public data class OpenRelated(
        val cardId: String,
    ) : CardDetailAction
}
