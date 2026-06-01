package app.sensee.feature.vocabularyEditor.presentation.impl

import app.sensee.ai.core.AiEnrichmentClient
import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.core.EnrichmentSuggestion
import app.sensee.ai.core.GrammarTagHint
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.feature.vocabularyEditor.domain.EntryId
import app.sensee.feature.vocabularyEditor.domain.EntryStatus
import app.sensee.feature.vocabularyEditor.domain.LexicalEntry
import app.sensee.feature.vocabularyEditor.domain.Meaning
import app.sensee.feature.vocabularyEditor.domain.MeaningCandidateId
import app.sensee.feature.vocabularyEditor.domain.SuggestVocabularyMeaningsUseCase
import app.sensee.feature.vocabularyEditor.domain.VocabularyRepository
import app.sensee.feature.vocabularyEditor.presentation.api.ManualSenseStatus
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarLabel
import app.sensee.grammar.domain.GrammarLabelForm
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarLabelsLoadResult
import app.sensee.grammar.domain.GrammarLabelsProvider
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.TaxonomyInvariants
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VocabularyCaptureLogicTest {
    private class FakeVocabularyRepository(
        private val confirmGate: CompletableDeferred<Unit>? = null,
    ) : VocabularyRepository {
        var lastConfirmed: Pair<EntryId, List<Meaning>>? = null
            private set
        var confirmCount = 0
            private set
        var createDraftCount = 0
            private set
        var updatedDraft: LexicalEntry? = null
            private set
        val deletedIds: MutableList<EntryId> = mutableListOf()

        override suspend fun createDraft(term: String): LexicalEntry {
            createDraftCount++
            return LexicalEntry(EntryId("e1"), term.trim(), EntryStatus.Draft)
        }

        override suspend fun updateDraftTerm(
            id: EntryId,
            term: String,
        ): LexicalEntry =
            LexicalEntry(id, term.trim(), EntryStatus.Draft)
                .also { updatedDraft = it }

        override suspend fun getEntry(id: EntryId): LexicalEntry? = null

        override suspend fun listEntries(): List<LexicalEntry> = emptyList()

        override fun observeEntries(): Flow<List<LexicalEntry>> = flowOf(emptyList())

        override suspend fun deleteEntry(id: EntryId) {
            deletedIds.add(id)
        }

        // confirmSenses is the abstract contract; confirmMeanings is a
        // transitional alias that delegates to it. The fake overrides
        // confirmSenses so both call sites go through one recorded path.
        override suspend fun confirmSenses(
            id: EntryId,
            senses: List<Meaning>,
        ): LexicalEntry {
            confirmCount++
            confirmGate?.await()
            lastConfirmed = id to senses
            return LexicalEntry(id, "come across", EntryStatus.Confirmed, senses)
        }
    }

    private class FakeAi(
        private val result: EnrichmentResult,
    ) : AiEnrichmentClient {
        override suspend fun enrich(request: EnrichmentRequest): EnrichmentResult = result
    }

    private val emptyLabelsProvider = GrammarLabelsProvider { GrammarLabels.EMPTY }
    private val noTaxonomyProvider = TaxonomyInvariantsProvider { null }

    private class StaticLabelsProvider(
        private val labels: GrammarLabels,
    ) : GrammarLabelsProvider {
        override suspend fun labels(): GrammarLabels = labels

        override fun cachedLabels(): GrammarLabels = labels

        override suspend fun awaitLabels(): GrammarLabelsLoadResult = GrammarLabelsLoadResult.Loaded(labels)
    }

    private fun logic(
        ai: AiEnrichmentClient,
        repo: VocabularyRepository,
        taxonomyProvider: TaxonomyInvariantsProvider = noTaxonomyProvider,
        labelsProvider: GrammarLabelsProvider = emptyLabelsProvider,
    ) = VocabularyCaptureLogic(
        enrichmentClient = ai,
        vocabularyRepository = repo,
        suggestMeanings = SuggestVocabularyMeaningsUseCase(repo, ai, taxonomyProvider, noOpAppDiagnostics()),
        grammarLabelsProvider = labelsProvider,
        taxonomyInvariantsProvider = taxonomyProvider,
        appDispatchers = immediateAppDispatchers(),
        appDiagnostics = noOpAppDiagnostics(),
    )

    private fun availableAi(vararg translations: String) =
        FakeAi(
            EnrichmentResult(
                availability = EnrichmentAvailability.Available,
                suggestions = translations.map { EnrichmentSuggestion(translation = it) },
            ),
        )

    private fun VocabularyCaptureLogic.candidateId(index: Int): MeaningCandidateId = uiState.value.candidates[index].id

    private fun VocabularyCaptureLogic.manualSuggestionId(
        manualIndex: Int,
        suggestionIndex: Int,
    ): MeaningCandidateId {
        val manualSense = uiState.value.manualSenses[manualIndex]
        return manualSense.suggestions[suggestionIndex].id
    }

    @Test
    fun `suggest populates candidates from the enrichment seam`() {
        val logic = logic(availableAi("наткнуться", "произвести впечатление"), FakeVocabularyRepository())

        logic.suggest(term = "come across")

        val state = logic.uiState.value
        assertEquals(DataLoadingState.Success, state.loadingState)
        assertEquals(
            listOf("наткнуться", "произвести впечатление"),
            state.candidates.map { it.translation },
        )
    }

    @Test
    fun `re-running suggest reuses the session draft instead of spawning a new row`() {
        val repo = FakeVocabularyRepository()
        val logic = logic(availableAi("наткнуться"), repo)

        logic.suggest(term = "come across")
        logic.suggest(term = "come across")
        logic.suggest(term = "come acros")

        assertEquals(1, repo.createDraftCount, "one draft per capture session, no orphan rows")
        assertEquals("come acros", repo.updatedDraft?.term)
    }

    @Test
    fun `multi-selecting senses and confirming saves exactly the selected meanings`() {
        val repo = FakeVocabularyRepository()
        val logic = logic(availableAi("наткнуться", "произвести впечатление", "ощущаться"), repo)
        logic.suggest(term = "come across")

        logic.toggleCandidate(logic.candidateId(0))
        logic.toggleCandidate(logic.candidateId(2))
        logic.confirmSelected()

        assertEquals(
            listOf("наткнуться", "ощущаться"),
            repo.lastConfirmed?.second?.map { it.translation },
        )
        assertEquals("come across", logic.uiState.value.confirmedTerm)
    }

    @Test
    fun `a second confirm while one is in flight does not append meanings twice`() {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeVocabularyRepository(confirmGate = gate)
        val logic = logic(availableAi("наткнуться"), repo)
        logic.suggest(term = "come across")
        logic.toggleCandidate(logic.candidateId(0))

        logic.confirmSelected()
        logic.confirmSelected()
        gate.complete(Unit)

        assertEquals(1, repo.confirmCount, "the in-flight confirm is not re-issued")
    }

    @Test
    fun `a manual variant is added alongside selected candidates`() {
        val repo = FakeVocabularyRepository()
        val logic = logic(availableAi("наткнуться"), repo)
        logic.suggest(term = "come across")

        logic.toggleCandidate(logic.candidateId(0))
        logic.addManual("  свой смысл  ")
        logic.confirmSelected()

        assertEquals(
            listOf("наткнуться", "свой смысл"),
            repo.lastConfirmed?.second?.map { it.translation },
        )
    }

    @Test
    fun `completing a manual sense surfaces assistant suggestions`() {
        val logic = logic(availableAi("come across"), FakeVocabularyRepository())
        logic.suggest(term = "come across")
        logic.addManual("свой смысл")

        logic.completeManualWithAssistant(manualIndex = 0)

        val state = logic.uiState.value
        val sense = state.manualSenses.single()
        assertEquals(ManualSenseStatus.Completed, sense.status)
        assertEquals(
            listOf("come across"),
            sense.suggestions.map { it.translation },
        )
    }

    @Test
    fun `completing a manual sense validates assistant taxonomy ids`() {
        val taxonomyProvider =
            TaxonomyInvariantsProvider {
                TaxonomyInvariants.EMPTY.copy(
                    allowedFormsByCategory = mapOf("number" to setOf("plural")),
                )
            }
        val ai =
            FakeAi(
                EnrichmentResult(
                    availability = EnrichmentAvailability.Available,
                    suggestions =
                        listOf(
                            EnrichmentSuggestion(
                                translation = "кошки",
                                grammarTags =
                                    listOf(
                                        GrammarTagHint("number", "plural"),
                                        GrammarTagHint("number", "past_tense"),
                                    ),
                            ),
                        ),
                ),
            )
        val logic = logic(ai, FakeVocabularyRepository(), taxonomyProvider)
        logic.suggest(term = "cats")
        logic.addManual("ручной смысл")

        logic.completeManualWithAssistant(manualIndex = 0)

        val suggestion =
            logic
                .uiState
                .value
                .manualSenses
                .single()
                .suggestions
                .single()
        assertEquals(
            listOf(GrammarTag(GrammarCategory.Number, GrammarForm.Plural)),
            suggestion.grammarTags,
        )
    }

    @Test
    fun `a picked assistant suggestion replaces the manual draft on confirm`() {
        val repo = FakeVocabularyRepository()
        val logic = logic(availableAi("enriched sense"), repo)
        logic.suggest(term = "come across")
        logic.addManual("мой черновик")
        logic.completeManualWithAssistant(manualIndex = 0)

        logic.toggleManualSuggestion(manualIndex = 0, suggestionId = logic.manualSuggestionId(0, 0))
        logic.confirmSelected()

        assertEquals(
            listOf("enriched sense"),
            repo.lastConfirmed?.second?.map { it.translation },
        )
    }

    @Test
    fun `a manual sense still confirms when the assistant is unavailable`() {
        val repo = FakeVocabularyRepository()
        val unavailableAi =
            FakeAi(EnrichmentResult(availability = EnrichmentAvailability.Unavailable("no key")))
        val logic = logic(unavailableAi, repo)
        logic.suggest(term = "come across")

        logic.addManual("ручной смысл")
        logic.completeManualWithAssistant(manualIndex = 0)

        val failedState = logic.uiState.value
        val failed = failedState.manualSenses.single()
        assertEquals(ManualSenseStatus.CompleteFailed, failed.status)

        logic.confirmSelected()

        assertEquals(
            listOf("ручной смысл"),
            repo.lastConfirmed?.second?.map { it.translation },
        )
        val confirmedManual = repo.lastConfirmed?.second?.single()
        assertTrue(confirmedManual?.surfaceForm == null)
    }

    @Test
    fun `confirming with nothing selected is a no-op`() {
        val repo = FakeVocabularyRepository()
        val logic = logic(availableAi("наткнуться"), repo)
        logic.suggest(term = "come across")

        logic.confirmSelected()

        assertEquals(null, repo.lastConfirmed)
    }

    @Test
    fun `a manual sense keeps its surface form and unit type through confirm`() {
        val repo = FakeVocabularyRepository()
        val logic = logic(availableAi(), repo)
        logic.suggest(term = "come across")

        logic.addManual(
            translation = "  наткнуться  ",
            surfaceForm = "come across [as]",
            unitType = GrammarUnitType.PhrasalVerb,
        )
        logic.confirmSelected()

        val confirmed = repo.lastConfirmed?.second?.single()
        assertEquals("наткнуться", confirmed?.translation)
        assertEquals(GrammarUnitType.PhrasalVerb, confirmed?.unitType)
        assertNotNull(confirmed?.surfaceForm)
    }

    @Test
    fun `toggling a manual suggestion off clears the selection`() {
        val logic = logic(availableAi("enriched sense"), FakeVocabularyRepository())
        logic.suggest(term = "come across")
        logic.addManual("мой черновик")
        logic.completeManualWithAssistant(manualIndex = 0)

        val suggestionId = logic.manualSuggestionId(0, 0)
        logic.toggleManualSuggestion(manualIndex = 0, suggestionId = suggestionId)
        logic.toggleManualSuggestion(manualIndex = 0, suggestionId = suggestionId)

        val state = logic.uiState.value
        val sense = state.manualSenses.single()
        assertTrue(sense.selectedSuggestions.isEmpty())
    }

    @Test
    fun `a completed manual sense with no suggestion picked confirms the thin draft`() {
        val repo = FakeVocabularyRepository()
        val logic = logic(availableAi("enriched sense"), repo)
        logic.suggest(term = "come across")
        logic.addManual("мой черновик")
        logic.completeManualWithAssistant(manualIndex = 0)

        logic.confirmSelected()

        assertEquals(
            listOf("мой черновик"),
            repo.lastConfirmed?.second?.map { it.translation },
        )
    }

    @Test
    fun `the assistant returning no senses marks the manual sense complete-failed`() {
        val logic = logic(availableAi(), FakeVocabularyRepository())
        logic.suggest(term = "come across")
        logic.addManual("мой черновик")

        logic.completeManualWithAssistant(manualIndex = 0)

        val state = logic.uiState.value
        val sense = state.manualSenses.single()
        assertEquals(ManualSenseStatus.CompleteFailed, sense.status)
    }

    @Test
    fun `toggling a candidate by an unknown id is a no-op`() {
        val logic = logic(availableAi("наткнуться"), FakeVocabularyRepository())
        logic.suggest(term = "come across")

        logic.toggleCandidate(MeaningCandidateId("not-a-real-candidate"))

        val state = logic.uiState.value
        assertTrue(state.selectedCandidates.isEmpty())
    }

    @Test
    fun `selection is keyed by candidate identity not list position`() {
        val repo = FakeVocabularyRepository()
        val logic = logic(availableAi("наткнуться", "произвести впечатление", "ощущаться"), repo)
        logic.suggest(term = "come across")

        // Select by the id of the middle sense; confirm must save exactly it,
        // independent of where it sits in the candidate list.
        val middleId = logic.candidateId(1)
        logic.toggleCandidate(middleId)
        logic.confirmSelected()

        assertEquals(
            listOf("произвести впечатление"),
            repo.lastConfirmed?.second?.map { it.translation },
        )
    }

    @Test
    fun `capture another preserves loaded grammar labels`() {
        val labels =
            GrammarLabels(
                unitTypeLabels =
                    mapOf(
                        "noun" to
                            mapOf(
                                "en" to GrammarLabel(long = "Noun"),
                            ),
                    ),
                categoryLabels = emptyMap(),
                formLabels = emptyMap(),
                formLabelsByName = emptyMap(),
                usageValueLabels = emptyMap(),
                complementLabels = emptyMap(),
            )
        val repo = FakeVocabularyRepository()
        val logic =
            logic(
                ai = availableAi("существительное"),
                repo = repo,
                labelsProvider = StaticLabelsProvider(labels),
            )
        logic.suggest(term = "thing")
        logic.toggleCandidate(logic.candidateId(0))
        logic.confirmSelected()

        logic.reset()

        val state = logic.uiState.value
        assertEquals(DataLoadingState.Success, state.grammarLabelsState)
        assertEquals("Noun", state.grammarLabels.unitType(GrammarUnitType.Noun, "en", GrammarLabelForm.Long))
    }
}
