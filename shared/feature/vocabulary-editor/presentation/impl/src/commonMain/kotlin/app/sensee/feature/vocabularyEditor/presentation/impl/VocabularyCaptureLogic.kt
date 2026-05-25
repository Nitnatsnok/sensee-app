package app.sensee.feature.vocabularyEditor.presentation.impl

import app.sensee.ai.core.AiEnrichmentClient
import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.ai.core.SenseCoverage
import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.decompose.logic.BaseLogic
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.vocabularyEditor.domain.EntryId
import app.sensee.feature.vocabularyEditor.domain.Meaning
import app.sensee.feature.vocabularyEditor.domain.MeaningCandidate
import app.sensee.feature.vocabularyEditor.domain.MeaningCandidateId
import app.sensee.feature.vocabularyEditor.domain.SuggestVocabularyMeaningsUseCase
import app.sensee.feature.vocabularyEditor.domain.VocabularyRepository
import app.sensee.feature.vocabularyEditor.domain.toMeaningCandidates
import app.sensee.feature.vocabularyEditor.domain.warnUnknownTaxonomyValue
import app.sensee.feature.vocabularyEditor.presentation.api.CaptureStatusNote
import app.sensee.feature.vocabularyEditor.presentation.api.ManualSense
import app.sensee.feature.vocabularyEditor.presentation.api.ManualSenseStatus
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyCaptureUiState
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarLabelsLoadResult
import app.sensee.grammar.domain.GrammarLabelsProvider
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AssistedInject
public class VocabularyCaptureLogic(
    private val enrichmentClient: AiEnrichmentClient,
    private val vocabularyRepository: VocabularyRepository,
    private val suggestMeanings: SuggestVocabularyMeaningsUseCase,
    private val grammarLabelsProvider: GrammarLabelsProvider,
    private val taxonomyInvariantsProvider: TaxonomyInvariantsProvider,
    appDispatchers: AppDispatchers,
    private val appDiagnostics: AppDiagnostics,
) : BaseLogic(appDispatchers, appDiagnostics) {
    private val onUnknownTaxonomyValue: (field: String, id: String) -> Unit =
        appDiagnostics::warnUnknownTaxonomyValue

    @AssistedFactory
    public fun interface Factory {
        public fun create(): VocabularyCaptureLogic
    }

    private val mutableUiState =
        grammarLabelsProvider.cachedLabels().let { initial ->
            MutableStateFlow(
                VocabularyCaptureUiState(
                    grammarLabels = initial,
                    grammarLabelsState = initialLabelsState(initial),
                ),
            )
        }
    public val uiState: StateFlow<VocabularyCaptureUiState> = mutableUiState.asStateFlow()

    private var draftId: EntryId? = null

    // Guards a re-entrant confirm: a second tap before the first
    // confirmMeanings round-trip resolves would append the same meanings twice.
    private var confirming = false

    init {
        loadGrammarLabels()
    }

    public fun retryGrammarLabels(): Unit = loadGrammarLabels()

    private fun loadGrammarLabels() {
        logicScope.launch {
            mutableUiState.update { it.copy(grammarLabelsState = DataLoadingState.Loading) }
            when (val result = grammarLabelsProvider.awaitLabels()) {
                is GrammarLabelsLoadResult.Loaded ->
                    mutableUiState.update {
                        it.copy(
                            grammarLabels = result.labels,
                            grammarLabelsState = DataLoadingState.Success,
                        )
                    }
                is GrammarLabelsLoadResult.Failed ->
                    mutableUiState.update {
                        it.copy(
                            grammarLabelsState =
                                DataLoadingState.Error(
                                    result.cause ?: IllegalStateException("grammar labels load failed"),
                                ),
                        )
                    }
            }
        }
    }

    public fun suggest(term: String) {
        val trimmed = term.trim()
        if (trimmed.isEmpty()) return
        logicScope.launch {
            mutableUiState.update {
                VocabularyCaptureUiState(
                    term = trimmed,
                    loadingState = DataLoadingState.Loading,
                    grammarLabels = it.grammarLabels,
                    grammarLabelsState = it.grammarLabelsState,
                )
            }
            runCatchingCancellable {
                suggestMeanings(term = trimmed, existingDraftId = draftId)
            }.onSuccess { suggestion ->
                draftId = suggestion.draftId
                val note: CaptureStatusNote? =
                    when (val availability = suggestion.availability) {
                        is EnrichmentAvailability.Unavailable -> CaptureStatusNote.AiUnavailable
                        is EnrichmentAvailability.Degraded -> CaptureStatusNote.AiDegraded(availability.reason)
                        EnrichmentAvailability.Available -> null
                    }
                mutableUiState.update {
                    it.copy(
                        loadingState = DataLoadingState.Success,
                        candidates = suggestion.candidates,
                        statusNote = note,
                    )
                }
            }.onFailure { throwable ->
                logger.error(throwable) { "Capture suggestion failed" }
                mutableUiState.update {
                    it.copy(loadingState = DataLoadingState.Error(throwable))
                }
            }
        }
    }

    public fun toggleCandidate(id: MeaningCandidateId) {
        mutableUiState.update { state ->
            if (state.candidates.none { it.id == id }) return@update state
            val selected = state.selectedCandidates
            state.copy(
                selectedCandidates =
                    if (id in selected) selected - id else selected + id,
            )
        }
    }

    public fun addManual(
        translation: String,
        surfaceForm: String? = null,
        unitType: GrammarUnitType? = null,
    ) {
        val trimmed = translation.trim()
        if (trimmed.isEmpty()) return
        val form = surfaceForm?.trim()?.takeIf { it.isNotEmpty() }?.let { SurfaceForm.parse(it) }
        val sense =
            ManualSense(
                meaning =
                    Meaning(
                        translation = trimmed,
                        surfaceForm = form,
                        unitType = unitType,
                    ),
            )
        mutableUiState.update { it.copy(manualSenses = it.manualSenses + sense) }
    }

    public fun completeManualWithAssistant(manualIndex: Int) {
        val state = mutableUiState.value
        val sense = state.manualSenses.getOrNull(manualIndex) ?: return
        if (sense.status == ManualSenseStatus.Completing) return
        // The studied unit to enrich: the user's surface form if given, else the
        // captured term; the native meaning is the disambiguating hint.
        val term =
            sense.meaning.surfaceForm
                ?.display()
                ?.takeIf { it.isNotBlank() }
                ?: state.term
        if (term.isBlank()) return
        updateManual(manualIndex) { it.copy(status = ManualSenseStatus.Completing) }
        logicScope.launch {
            runCatchingCancellable {
                enrichAndMap(
                    request =
                        EnrichmentRequest(
                            term = term,
                            userNote = sense.meaning.translation,
                            // We want the one sense the user described; Minimal also
                            // suppresses the "look for more senses" corrective retry.
                            senseCoverage = SenseCoverage.Minimal,
                        ),
                    fallbackTerm = term,
                )
            }.onSuccess { suggestions ->
                updateManual(manualIndex) {
                    if (suggestions.isEmpty()) {
                        it.copy(status = ManualSenseStatus.CompleteFailed)
                    } else {
                        it.copy(
                            status = ManualSenseStatus.Completed,
                            suggestions = suggestions,
                            selectedSuggestions = emptySet(),
                        )
                    }
                }
            }.onFailure { throwable ->
                logger.error(throwable) { "Completing manual sense failed" }
                updateManual(manualIndex) { it.copy(status = ManualSenseStatus.CompleteFailed) }
            }
        }
    }

    /** Shared mapping path for AI-driven flows that don't go through [suggestMeanings]. */
    private suspend fun enrichAndMap(
        request: EnrichmentRequest,
        fallbackTerm: String,
    ): List<MeaningCandidate> =
        enrichmentClient
            .enrich(request)
            .toMeaningCandidates(
                fallbackTerm = fallbackTerm,
                invariants = taxonomyInvariantsProvider.invariants(),
                onUnknown = onUnknownTaxonomyValue,
            )

    public fun toggleManualSuggestion(
        manualIndex: Int,
        suggestionId: MeaningCandidateId,
    ) {
        updateManual(manualIndex) { sense ->
            if (sense.suggestions.none { it.id == suggestionId }) return@updateManual sense
            val selected = sense.selectedSuggestions
            sense.copy(
                selectedSuggestions =
                    if (suggestionId in selected) {
                        selected - suggestionId
                    } else {
                        selected + suggestionId
                    },
            )
        }
    }

    public fun confirmSelected() {
        val id = draftId ?: return
        val state = mutableUiState.value
        val fromCandidates =
            state.candidates
                .filter { it.id in state.selectedCandidates }
                .map { it.toConfirmedMeaning() }
        val fromManual =
            state.manualSenses.flatMap { sense ->
                // A picked enriched suggestion replaces the thin draft; with
                // none picked the hand-authored sense stands on its own.
                if (sense.selectedSuggestions.isEmpty()) {
                    listOf(sense.meaning)
                } else {
                    sense.suggestions
                        .filter { it.id in sense.selectedSuggestions }
                        .map { it.toConfirmedMeaning() }
                }
            }
        val meanings = fromCandidates + fromManual
        if (meanings.isEmpty() || confirming) return
        confirming = true
        logicScope.launch {
            try {
                runCatchingCancellable { vocabularyRepository.confirmMeanings(id, meanings) }
                    .onSuccess { entry ->
                        mutableUiState.update {
                            it.freshSessionState(confirmedTerm = entry.term)
                        }
                    }.onFailure { throwable ->
                        logger.error(throwable) { "Confirming meanings failed" }
                        crashReporter.recordException(
                            throwable,
                            attributes =
                                mapOf(
                                    "area" to "vocabulary_capture",
                                    "operation" to "confirm_meanings",
                                    "draft_id" to id.value,
                                    "meaning_count" to meanings.size.toString(),
                                ),
                        )
                    }
            } finally {
                confirming = false
            }
        }
    }

    public fun reset() {
        draftId = null
        mutableUiState.update { it.freshSessionState() }
    }

    private inline fun updateManual(
        index: Int,
        transform: (ManualSense) -> ManualSense,
    ) {
        mutableUiState.update { state ->
            if (index !in state.manualSenses.indices) return@update state
            state.copy(
                manualSenses =
                    state.manualSenses.mapIndexed { i, sense ->
                        if (i == index) transform(sense) else sense
                    },
            )
        }
    }
}

private fun initialLabelsState(seed: GrammarLabels): DataLoadingState =
    if (seed === GrammarLabels.EMPTY) DataLoadingState.Idle else DataLoadingState.Success

private fun VocabularyCaptureUiState.freshSessionState(confirmedTerm: String? = null): VocabularyCaptureUiState =
    VocabularyCaptureUiState(
        confirmedTerm = confirmedTerm,
        grammarLabels = grammarLabels,
        grammarLabelsState = grammarLabelsState,
        studyLanguageTag = studyLanguageTag,
        nativeLanguageTag = nativeLanguageTag,
    )
