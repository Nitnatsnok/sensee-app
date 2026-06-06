package app.sensee.feature.vocabularyEditor.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.presentation.DataLoadingState
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.deriveSenseContentKey
import app.sensee.lexicon.domain.isConfirmable
import kotlinx.coroutines.flow.StateFlow

public interface VocabularyCaptureComponent : AppComponent {
    public val uiState: StateFlow<VocabularyCaptureUiState>

    public fun onAction(action: VocabularyCaptureAction)

    public fun interface Factory {
        public fun create(componentContext: AppComponentContext): VocabularyCaptureComponent
    }
}

/**
 * Quick-capture state. The flow is not a stage wizard (ADR-001): enrich returns
 * a set of senses, the user multi-selects which to add and may add their own
 * ([manualSenses]); confirming the selection IS the confirmation. [loadingState]
 * tracks the enrichment request lifecycle (an actual failure is
 * [DataLoadingState.Error]); [statusNote] is distinct — a first-class,
 * *successful* seam degradation (ADR-005: Unavailable / Degraded) where the user
 * still adds a meaning manually.
 *
 * AI suggestions and hand-authored senses are the same [CaptureSense] type;
 * selection is keyed by [CaptureSense.contentKey].
 */
public data class VocabularyCaptureUiState(
    val term: String = "",
    val loadingState: DataLoadingState = DataLoadingState.Idle,
    val candidates: List<CaptureSense> = emptyList(),
    val manualSenses: List<CaptureSense> = emptyList(),
    val statusNote: CaptureStatusNote? = null,
    val confirmedTerm: String? = null,
    val grammarLabels: GrammarLabels = GrammarLabels.EMPTY,
    /** Per-screen lifecycle of the grammar-label dictionary load. */
    val grammarLabelsState: DataLoadingState = DataLoadingState.Idle,
    /**
     * BCP-47 tag of the language the learner is studying — drives short
     * (study-language) badge labels on the sense card.
     */
    val studyLanguageTag: String = "en",
    /**
     * BCP-47 tag of the learner's native language — drives long
     * (native-language) labels in the detail panel and the manual-sense block.
     */
    val nativeLanguageTag: String = "ru",
) {
    public val canConfirm: Boolean
        get() = selectedSenses().any { it.isConfirmable() }

    /**
     * The senses the user has lined up to confirm: each selected AI candidate plus,
     * per manual entry, its picked assistant suggestions — or the hand-authored
     * sense when none is picked. Not yet gated or de-duplicated: confirm keeps only
     * the confirmable ones ([Sense.isConfirmable]) and de-dups by content key, so a
     * thin manual sense with no example can be added but is not yet confirmable.
     */
    public fun selectedSenses(): List<Sense> =
        candidates.filter { it.selected }.map { it.sense } +
            manualSenses.flatMap { manual ->
                val picked = manual.assistantSuggestions.filter { it.selected }
                if (picked.isEmpty()) listOf(manual.sense) else picked.map { it.sense }
            }
}

/**
 * One in-progress sense in a capture session — a single type for both
 * AI-proposed and hand-authored senses (ADR-001: a candidate is just an unsaved
 * [Sense] the user may confirm; the selection IS the confirmation). Identity for
 * selection is the [contentKey], so a re-enrich that returns the same sense
 * keeps its selection instead of shifting with list position.
 *
 * For a [CaptureSenseSource.Manual] sense, [assistantSuggestions] are optional
 * AI completions; picking one (its [selected]) confirms it instead of the thin
 * manual sense. A manual sense with no picked suggestion is confirmed as-is.
 */
public data class CaptureSense(
    val sense: Sense,
    val source: CaptureSenseSource,
    val status: CaptureSenseStatus = CaptureSenseStatus.Ready,
    val selected: Boolean = false,
    val assistantSuggestions: List<CaptureSense> = emptyList(),
) {
    public val contentKey: String get() = deriveSenseContentKey(sense)
}

public enum class CaptureSenseSource {
    /** Proposed by the AI seam. */
    Ai,

    /** Hand-authored by the user. */
    Manual,
}

public enum class CaptureSenseStatus {
    /** A settled sense: an AI candidate, or a manual sense not being completed. */
    Ready,

    /** A "complete with assistant" re-enrich is in flight (manual only). */
    Completing,

    /** The assistant could not complete a manual sense; the thin draft stands. */
    CompleteFailed,
}

/**
 * First-class seam-degradation note for the capture screen. The AI seam still
 * returned a usable result (no [DataLoadingState.Error]), but the user should
 * know either there is no AI configured ([AiUnavailable]) or that the answer
 * was partial ([AiDegraded]). The reason text from the seam is the only thing
 * not localised — it comes from the provider for diagnostics.
 */
public sealed interface CaptureStatusNote {
    public data object AiUnavailable : CaptureStatusNote

    public data class AiDegraded(
        val reason: String,
    ) : CaptureStatusNote
}

public sealed interface VocabularyCaptureAction {
    public data class Suggest(
        val term: String,
    ) : VocabularyCaptureAction

    /** Toggle selection of the AI candidate identified by [contentKey]. */
    public data class ToggleCandidate(
        val contentKey: String,
    ) : VocabularyCaptureAction

    /**
     * Add a hand-authored sense. [translation] is required (native language);
     * [surfaceForm] (studied form text, parsed structurally downstream), [unitType],
     * and [example] (one usage sentence) are optional refinements. None require AI —
     * but a manual sense needs at least one example (typed here or added by the
     * assistant) before it is confirmable.
     */
    public data class AddManual(
        val translation: String,
        val surfaceForm: String? = null,
        val unitType: GrammarUnitType? = null,
        val example: String? = null,
    ) : VocabularyCaptureAction

    /** Optionally enrich the manual sense at [manualIndex] via the AI seam. */
    public data class CompleteManualWithAssistant(
        val manualIndex: Int,
    ) : VocabularyCaptureAction

    /** Pick/unpick an assistant suggestion for a completed manual sense. */
    public data class ToggleManualSuggestion(
        val manualIndex: Int,
        val suggestionContentKey: String,
    ) : VocabularyCaptureAction

    public data object ConfirmSelected : VocabularyCaptureAction

    public data object Reset : VocabularyCaptureAction

    public data object RetryGrammarLabels : VocabularyCaptureAction
}
