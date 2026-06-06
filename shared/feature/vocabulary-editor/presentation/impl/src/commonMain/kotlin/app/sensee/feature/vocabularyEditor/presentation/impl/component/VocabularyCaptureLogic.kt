package app.sensee.feature.vocabularyEditor.presentation.impl.component

import app.sensee.ai.core.contract.EnrichmentAvailability
import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.decompose.logic.BaseLogic
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.vocabularyEditor.domain.usecase.ConfirmSensesUseCase
import app.sensee.feature.vocabularyEditor.domain.usecase.SuggestVocabularySensesUseCase
import app.sensee.feature.vocabularyEditor.presentation.api.CaptureSense
import app.sensee.feature.vocabularyEditor.presentation.api.CaptureSenseSource
import app.sensee.feature.vocabularyEditor.presentation.api.CaptureSenseStatus
import app.sensee.feature.vocabularyEditor.presentation.api.CaptureStatusNote
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyCaptureUiState
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarLabelsLoadResult
import app.sensee.grammar.domain.GrammarLabelsProvider
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.lexicon.domain.ContextualApplication
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.deriveSenseContentKey
import app.sensee.lexicon.domain.isConfirmable
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AssistedInject
public class VocabularyCaptureLogic(
    private val confirmSenses: ConfirmSensesUseCase,
    private val suggestSenses: SuggestVocabularySensesUseCase,
    private val grammarLabelsProvider: GrammarLabelsProvider,
    appDispatchers: AppDispatchers,
    private val appDiagnostics: AppDiagnostics,
) : BaseLogic(appDispatchers, appDiagnostics) {
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

    // Guards a re-entrant confirm: a second tap before the first confirmAll
    // round-trip resolves would write the same senses twice.
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
                    studyLanguageTag = it.studyLanguageTag,
                    nativeLanguageTag = it.nativeLanguageTag,
                )
            }
            runCatchingCancellable {
                suggestSenses(term = trimmed)
            }.onSuccess { suggestion ->
                mutableUiState.update {
                    it.copy(
                        loadingState = DataLoadingState.Success,
                        candidates =
                            suggestion.senses.map { sense ->
                                CaptureSense(sense = sense, source = CaptureSenseSource.Ai)
                            },
                        statusNote = noteFor(suggestion.availability),
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

    public fun toggleCandidate(contentKey: String) {
        mutableUiState.update { state ->
            if (state.candidates.none { it.contentKey == contentKey }) return@update state
            state.copy(candidates = state.candidates.toggleSelection(contentKey))
        }
    }

    public fun addManual(
        translation: String,
        surfaceForm: String? = null,
        unitType: GrammarUnitType? = null,
        example: String? = null,
    ) {
        val trimmed = translation.trim()
        if (trimmed.isEmpty()) return
        val form = surfaceForm?.trim()?.takeIf { it.isNotEmpty() }?.let { SurfaceForm.parse(it) }
        // A typed example makes the manual sense confirmable offline; without one it
        // stays a thin draft the confirm-gate holds back until the assistant fills it.
        val applications =
            example
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { listOf(ContextualApplication(StudiedSentence.parse(it))) }
                .orEmpty()
        val manual =
            CaptureSense(
                sense =
                    Sense(
                        translation = trimmed,
                        surfaceForm = form,
                        unitType = unitType,
                        contextualApplications = applications,
                    ),
                source = CaptureSenseSource.Manual,
            )
        mutableUiState.update { it.copy(manualSenses = it.manualSenses + manual) }
    }

    public fun completeManualWithAssistant(manualIndex: Int) {
        val state = mutableUiState.value
        val manual = state.manualSenses.getOrNull(manualIndex) ?: return
        if (manual.status == CaptureSenseStatus.Completing) return
        // The studied unit to enrich: the user's surface form if given, else the
        // captured term; the native meaning is the disambiguating hint.
        val term =
            manual.sense.surfaceForm
                ?.display()
                ?.takeIf { it.isNotBlank() }
                ?: state.term
        if (term.isBlank()) return
        updateManual(manualIndex) { it.copy(status = CaptureSenseStatus.Completing) }
        logicScope.launch {
            runCatchingCancellable {
                suggestSenses.completeManualSense(term = term, userNote = manual.sense.translation)
            }.onSuccess { senses ->
                updateManual(manualIndex) {
                    if (senses.isEmpty()) {
                        it.copy(status = CaptureSenseStatus.CompleteFailed)
                    } else {
                        it.copy(
                            status = CaptureSenseStatus.Ready,
                            assistantSuggestions =
                                senses.map { sense ->
                                    CaptureSense(sense = sense, source = CaptureSenseSource.Ai)
                                },
                        )
                    }
                }
            }.onFailure { throwable ->
                logger.error(throwable) { "Completing manual sense failed" }
                updateManual(manualIndex) { it.copy(status = CaptureSenseStatus.CompleteFailed) }
            }
        }
    }

    public fun toggleManualSuggestion(
        manualIndex: Int,
        suggestionContentKey: String,
    ) {
        updateManual(manualIndex) { manual ->
            if (manual.assistantSuggestions.none { it.contentKey == suggestionContentKey }) {
                return@updateManual manual
            }
            manual.copy(assistantSuggestions = manual.assistantSuggestions.toggleSelection(suggestionContentKey))
        }
    }

    public fun confirmSelected() {
        val state = mutableUiState.value
        // Keep only confirmable senses — a thin manual sense with no example cannot
        // be confirmed and stays in the form — then dedup before write: two
        // selections sharing a content key are the same sense, so persisting both
        // would fork its SRS state. Filter before dedup so a confirmable candidate
        // is never shadowed by a non-confirmable manual sense with the same key.
        val senses =
            state
                .selectedSenses()
                .filter { it.isConfirmable() }
                .distinctBy(::deriveSenseContentKey)
        if (senses.isEmpty() || confirming) return
        confirming = true
        val confirmedTerm = state.term
        logicScope.launch {
            try {
                runCatchingCancellable {
                    // Delegate the transactional confirm and best-effort embedding to the
                    // domain use-case; a re-capture reuses each sense_id (keeps its SRS).
                    confirmSenses(senses)
                }.onSuccess {
                    mutableUiState.update { state -> state.freshSessionState(confirmedTerm = confirmedTerm) }
                }.onFailure { throwable ->
                    logger.error(throwable) { "Confirming senses failed" }
                    crashReporter.recordException(
                        throwable,
                        attributes =
                            mapOf(
                                "area" to "vocabulary_capture",
                                "operation" to "confirm_senses",
                                "sense_count" to senses.size.toString(),
                            ),
                    )
                }
            } finally {
                confirming = false
            }
        }
    }

    public fun reset() {
        mutableUiState.update { it.freshSessionState() }
    }

    private inline fun updateManual(
        index: Int,
        transform: (CaptureSense) -> CaptureSense,
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

private fun List<CaptureSense>.toggleSelection(contentKey: String): List<CaptureSense> =
    map { if (it.contentKey == contentKey) it.copy(selected = !it.selected) else it }

private fun noteFor(availability: EnrichmentAvailability): CaptureStatusNote? =
    when (availability) {
        is EnrichmentAvailability.Unavailable -> CaptureStatusNote.AiUnavailable
        is EnrichmentAvailability.Degraded -> CaptureStatusNote.AiDegraded(availability.reason)
        EnrichmentAvailability.Available -> null
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
