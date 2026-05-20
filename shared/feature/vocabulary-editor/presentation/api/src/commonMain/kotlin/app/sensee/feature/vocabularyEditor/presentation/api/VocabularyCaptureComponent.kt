package app.sensee.feature.vocabularyEditor.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.vocabularyEditor.domain.Meaning
import app.sensee.feature.vocabularyEditor.domain.MeaningCandidate
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarUnitType
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
 * a set of senses, the user multi-selects which to add ([selectedCandidates])
 * and may add their own ([manualSenses]); confirming the selection IS the
 * confirmation. [loadingState] tracks the enrichment request lifecycle (an
 * actual failure is [DataLoadingState.Error]); [statusNote] is distinct — a
 * first-class, *successful* seam degradation (ADR-005: Unavailable / Degraded)
 * where the user still adds a meaning manually.
 */
public data class VocabularyCaptureUiState(
    val term: String = "",
    val loadingState: DataLoadingState = DataLoadingState.Idle,
    val candidates: List<MeaningCandidate> = emptyList(),
    val selectedCandidates: Set<Int> = emptySet(),
    val manualSenses: List<ManualSense> = emptyList(),
    val statusNote: String? = null,
    val confirmedTerm: String? = null,
    val grammarLabels: GrammarLabels = GrammarLabels.EMPTY,
) {
    public val canConfirm: Boolean
        get() = selectedCandidates.isNotEmpty() || manualSenses.isNotEmpty()
}

/**
 * A sense the user authored by hand. It is intentionally minimal — a
 * [translation][Meaning.translation] plus optional surface form / part of
 * speech — and is valid on its own: it reaches `Confirmed` with no AI at all
 * (the `vocabulary-capture.feature` degradation guarantee). It can *optionally*
 * be completed by the assistant ([status] / [suggestions]): a re-enrich with
 * the user's text as a hint surfaces enriched candidates the user confirms via
 * the same select model (ADR-001 — AI is a candidate, selection is the
 * confirmation). If the user selects an enriched suggestion, that one is
 * confirmed instead of the thin [meaning]; otherwise the thin [meaning] stands.
 */
public data class ManualSense(
    val meaning: Meaning,
    val status: ManualSenseStatus = ManualSenseStatus.Draft,
    val suggestions: List<MeaningCandidate> = emptyList(),
    val selectedSuggestions: Set<Int> = emptySet(),
)

public enum class ManualSenseStatus {
    /** Just added; thin, not (yet) completed by the assistant. */
    Draft,

    /** A "complete with assistant" re-enrich is in flight. */
    Completing,

    /** The assistant returned [ManualSense.suggestions]. */
    Completed,

    /** The assistant could not complete it; the thin draft still stands. */
    CompleteFailed,
}

public sealed interface VocabularyCaptureAction {
    public data class Suggest(
        val term: String,
    ) : VocabularyCaptureAction

    public data class ToggleCandidate(
        val index: Int,
    ) : VocabularyCaptureAction

    /**
     * Add a hand-authored sense. [translation] is required (native language);
     * [surfaceForm] (studied form text, parsed structurally downstream) and
     * [unitType] are optional refinements. None of these require AI.
     */
    public data class AddManual(
        val translation: String,
        val surfaceForm: String? = null,
        val unitType: GrammarUnitType? = null,
    ) : VocabularyCaptureAction

    /** Optionally enrich the manual sense at [manualIndex] via the AI seam. */
    public data class CompleteManualWithAssistant(
        val manualIndex: Int,
    ) : VocabularyCaptureAction

    /** Pick/unpick an assistant suggestion for a completed manual sense. */
    public data class ToggleManualSuggestion(
        val manualIndex: Int,
        val suggestionIndex: Int,
    ) : VocabularyCaptureAction

    public data object ConfirmSelected : VocabularyCaptureAction

    public data object Reset : VocabularyCaptureAction
}
