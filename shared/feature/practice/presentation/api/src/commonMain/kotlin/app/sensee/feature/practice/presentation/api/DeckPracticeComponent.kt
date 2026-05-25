package app.sensee.feature.practice.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.practice.domain.PracticeCardFront
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalDecomposeApi::class)
public typealias DeckPracticeChildPanels =
    ChildPanels<
        DeckPracticePanelConfig.Deck,
        AppComponent,
        DeckPracticePanelConfig.CardDetail,
        CardDetailComponent,
        Nothing,
        Nothing,
    >

@OptIn(ExperimentalDecomposeApi::class)
public interface DeckPracticeComponent : AppComponent {
    public val uiState: StateFlow<DeckPracticeUiState>

    public val panels: Value<DeckPracticeChildPanels>

    public fun onAction(action: DeckPracticeAction)

    public data class Args(
        val deckId: String,
        val focusedCardId: String? = null,
    )

    public fun interface Factory {
        public fun create(
            componentContext: AppComponentContext,
            args: Args,
        ): DeckPracticeComponent
    }
}

public data class DeckPracticeUiState(
    val loadingState: DataLoadingState = DataLoadingState.Loading,
    val deckTitle: String = "",
    val cards: PersistentList<DeckPracticeCardUiState> = persistentListOf(),
    // Cards closed (graduated out of the session) — not a swipe count: a card
    // counts once when FSRS graduates it, never on an in-session reinjection.
    val completedCount: Int = 0,
    val tapToFlipEnabled: Boolean = true,
    val grammarLabels: GrammarLabels = GrammarLabels.EMPTY,
    /**
     * Lifecycle of the grammar-label dictionary for *this* screen. Cold start
     * after the splash → already `Success`; warm-restore deep-link → starts
     * `Loading` and transitions to `Success`/`Error` as `awaitLabels` returns.
     * UI may render a skeleton + retry around the badges based on this.
     */
    val grammarLabelsState: DataLoadingState = DataLoadingState.Idle,
    /** BCP-47 tag of the language the learner is studying — drives short badge labels. */
    val studyLanguageTag: String = "en",
    /** BCP-47 tag of the learner's native language — drives long help-glossary descriptions. */
    val nativeLanguageTag: String = "ru",
)

public data class DeckPracticeCardUiState(
    // Identifies one *showing* of a card. The same [id] can appear multiple times in a session
    // (reinjected by the SRS scheduler), each time as a distinct presentation with its own flip
    // state and possibly a flipped front direction — so the deck must key on this, not [id].
    val presentationKey: String,
    val id: String,
    val lemmaId: String,
    val headword: String,
    val translation: String,
    val contextSentence: String,
    val unitType: GrammarUnitType,
    val grammarTags: PersistentList<GrammarTag> = persistentListOf(),
    val senseSummary: String,
    val explanation: String,
    val practiceFront: PracticeCardFront,
    // Set when this is the last card returning to an emptied deck; the screen plays a
    // one-shot slide-in for it. Decided in the logic so it arrives atomically with `cards`.
    val animateEntrance: Boolean = false,
)

public enum class DeckPracticeRatingAction {
    Again,
    Hard,
    Good,
    Easy,
}

public sealed interface DeckPracticeAction {
    public data object Retry : DeckPracticeAction

    /** Retry only the grammar-label dictionary load (the screen's badges). */
    public data object RetryGrammarLabels : DeckPracticeAction

    public data object ToggleTapToFlip : DeckPracticeAction

    public data class SubmitReview(
        val cardId: String,
        val rating: DeckPracticeRatingAction,
    ) : DeckPracticeAction

    public data class FocusCard(
        val cardId: String,
    ) : DeckPracticeAction

    public data object DismissDetails : DeckPracticeAction

    public data class SpeakText(
        val text: String,
    ) : DeckPracticeAction

    public data object Close : DeckPracticeAction

    public data object OpenHelp : DeckPracticeAction
}
