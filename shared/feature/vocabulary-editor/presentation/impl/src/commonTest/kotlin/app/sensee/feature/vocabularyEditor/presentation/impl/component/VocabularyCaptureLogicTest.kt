package app.sensee.feature.vocabularyEditor.presentation.impl.component

import app.sensee.ai.core.contract.AiEnrichmentClient
import app.sensee.ai.core.contract.EnrichmentAvailability
import app.sensee.ai.core.contract.EnrichmentResult
import app.sensee.ai.core.model.EnrichmentSuggestion
import app.sensee.ai.core.model.GrammarTagHint
import app.sensee.ai.core.request.EnrichmentRequest
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.feature.vocabularyEditor.domain.usecase.SuggestVocabularySensesUseCase
import app.sensee.feature.vocabularyEditor.presentation.api.CaptureSenseStatus
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
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.SenseId
import app.sensee.lexicon.domain.SenseOrigin
import app.sensee.lexicon.domain.SenseStatus
import app.sensee.lexicon.domain.SenseWriteRepository
import app.sensee.lexicon.domain.StoredSense
import app.sensee.lexicon.domain.WriteIntent
import app.sensee.lexicon.domain.deriveLemmaKey
import app.sensee.verification.core.contract.ExampleCheckRequest
import app.sensee.verification.core.contract.ExampleCheckResult
import app.sensee.verification.core.contract.ExampleQualityChecker
import app.sensee.verification.core.contract.LexicalVerificationQuery
import app.sensee.verification.core.contract.LexicalVerificationReport
import app.sensee.verification.core.contract.LexicalVerifier
import app.sensee.verification.core.contract.VerifierAvailability
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VocabularyCaptureLogicTest {
    // Records confirmed senses; the gate lets a test observe an in-flight confirm.
    private class FakeSenseWriteRepository(
        private val gate: CompletableDeferred<Unit>? = null,
    ) : SenseWriteRepository {
        val upserted: MutableList<Sense> = mutableListOf()
        var upsertCalls = 0
            private set

        override suspend fun upsert(
            sense: Sense,
            status: SenseStatus,
            origin: SenseOrigin,
            sourceRef: String?,
            intent: WriteIntent,
        ): StoredSense {
            upsertCalls++
            gate?.await()
            upserted += sense
            return StoredSense(
                id = SenseId("sense-${upserted.size}"),
                status = status,
                origin = origin,
                sourceRef = sourceRef,
                lemmaKey = deriveLemmaKey(sense),
                updatedAtEpochMs = 0L,
                sense = sense,
            )
        }

        override suspend fun confirmAll(senses: List<Sense>): List<StoredSense> =
            senses.map { upsert(it, SenseStatus.Confirmed, SenseOrigin.Personal) }
    }

    private class FakeAi(
        private val result: EnrichmentResult,
    ) : AiEnrichmentClient {
        override suspend fun enrich(request: EnrichmentRequest): EnrichmentResult = result
    }

    private object UnavailableVerifier : LexicalVerifier {
        override suspend fun verify(query: LexicalVerificationQuery): LexicalVerificationReport =
            LexicalVerificationReport.unavailable("test-only: no verifier wired")
    }

    private object NoOpExampleQualityChecker : ExampleQualityChecker {
        override suspend fun check(request: ExampleCheckRequest): ExampleCheckResult =
            ExampleCheckResult(VerifierAvailability.Available, emptyList(), rewrite = null, sources = emptyList())
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
        repo: FakeSenseWriteRepository = FakeSenseWriteRepository(),
        taxonomyProvider: TaxonomyInvariantsProvider = noTaxonomyProvider,
        labelsProvider: GrammarLabelsProvider = emptyLabelsProvider,
    ) = VocabularyCaptureLogic(
        senseWriteRepository = repo,
        suggestSenses =
            SuggestVocabularySensesUseCase(
                ai,
                UnavailableVerifier,
                setOf(NoOpExampleQualityChecker),
                taxonomyProvider,
                noOpAppDiagnostics(),
            ),
        grammarLabelsProvider = labelsProvider,
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

    private fun VocabularyCaptureLogic.candidateContentKey(index: Int): String =
        uiState.value.candidates[index].contentKey

    private fun VocabularyCaptureLogic.manualSuggestionContentKey(
        manualIndex: Int,
        suggestionIndex: Int,
    ): String =
        uiState.value.manualSenses[manualIndex]
            .assistantSuggestions[suggestionIndex]
            .contentKey

    @Test
    fun `suggest populates candidates from the enrichment seam`() {
        val logic = logic(availableAi("наткнуться", "произвести впечатление"))

        logic.suggest(term = "come across")

        val state = logic.uiState.value
        assertEquals(DataLoadingState.Success, state.loadingState)
        assertEquals(
            listOf("наткнуться", "произвести впечатление"),
            state.candidates.map { it.sense.translation },
        )
    }

    @Test
    fun `multi-selecting senses and confirming saves exactly the selected senses`() {
        val repo = FakeSenseWriteRepository()
        val logic = logic(availableAi("наткнуться", "произвести впечатление", "ощущаться"), repo)
        logic.suggest(term = "come across")

        logic.toggleCandidate(logic.candidateContentKey(0))
        logic.toggleCandidate(logic.candidateContentKey(2))
        logic.confirmSelected()

        assertEquals(listOf("наткнуться", "ощущаться"), repo.upserted.map { it.translation })
        assertEquals("come across", logic.uiState.value.confirmedTerm)
    }

    @Test
    fun `a second confirm while one is in flight does not write senses twice`() {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeSenseWriteRepository(gate = gate)
        val logic = logic(availableAi("наткнуться"), repo)
        logic.suggest(term = "come across")
        logic.toggleCandidate(logic.candidateContentKey(0))

        logic.confirmSelected()
        logic.confirmSelected()
        gate.complete(Unit)

        assertEquals(1, repo.upsertCalls, "the in-flight confirm is not re-issued")
    }

    @Test
    fun `a manual variant is added alongside selected candidates`() {
        val repo = FakeSenseWriteRepository()
        val logic = logic(availableAi("наткнуться"), repo)
        logic.suggest(term = "come across")

        logic.toggleCandidate(logic.candidateContentKey(0))
        logic.addManual("  свой смысл  ")
        logic.confirmSelected()

        assertEquals(listOf("наткнуться", "свой смысл"), repo.upserted.map { it.translation })
    }

    @Test
    fun `confirm dedups a selected candidate and a manual sense sharing a content key`() {
        val repo = FakeSenseWriteRepository()
        val logic = logic(availableAi("дубль"), repo)
        logic.suggest(term = "come across")

        logic.toggleCandidate(logic.candidateContentKey(0))
        // Same surface form (capture fallback term) + translation as the
        // candidate → same content key, so confirm must persist it once.
        logic.addManual(translation = "дубль", surfaceForm = "come across")
        logic.confirmSelected()

        assertEquals(listOf("дубль"), repo.upserted.map { it.translation })
    }

    @Test
    fun `completing a manual sense surfaces assistant suggestions`() {
        val logic = logic(availableAi("come across"))
        logic.suggest(term = "come across")
        logic.addManual("свой смысл")

        logic.completeManualWithAssistant(manualIndex = 0)

        val sense =
            logic.uiState.value.manualSenses
                .single()
        assertEquals(CaptureSenseStatus.Ready, sense.status)
        assertEquals(
            listOf("come across"),
            sense.assistantSuggestions.map { it.sense.translation },
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
        val logic = logic(ai, taxonomyProvider = taxonomyProvider)
        logic.suggest(term = "cats")
        logic.addManual("ручной смысл")

        logic.completeManualWithAssistant(manualIndex = 0)

        val suggestion =
            logic
                .uiState
                .value
                .manualSenses
                .single()
                .assistantSuggestions
                .single()
        assertEquals(
            listOf(GrammarTag(GrammarCategory.Number, GrammarForm.Plural)),
            suggestion.sense.grammarTags,
        )
    }

    @Test
    fun `a picked assistant suggestion replaces the manual draft on confirm`() {
        val repo = FakeSenseWriteRepository()
        val logic = logic(availableAi("enriched sense"), repo)
        logic.suggest(term = "come across")
        logic.addManual("мой черновик")
        logic.completeManualWithAssistant(manualIndex = 0)

        logic.toggleManualSuggestion(manualIndex = 0, suggestionContentKey = logic.manualSuggestionContentKey(0, 0))
        logic.confirmSelected()

        assertEquals(listOf("enriched sense"), repo.upserted.map { it.translation })
    }

    @Test
    fun `a manual sense still confirms when the assistant is unavailable`() {
        val repo = FakeSenseWriteRepository()
        val unavailableAi =
            FakeAi(EnrichmentResult(availability = EnrichmentAvailability.Unavailable("no key")))
        val logic = logic(unavailableAi, repo)
        logic.suggest(term = "come across")

        logic.addManual("ручной смысл")
        logic.completeManualWithAssistant(manualIndex = 0)

        val failed =
            logic.uiState.value.manualSenses
                .single()
        assertEquals(CaptureSenseStatus.CompleteFailed, failed.status)

        logic.confirmSelected()

        assertEquals(listOf("ручной смысл"), repo.upserted.map { it.translation })
        assertTrue(repo.upserted.single().surfaceForm == null)
    }

    @Test
    fun `confirming with nothing selected is a no-op`() {
        val repo = FakeSenseWriteRepository()
        val logic = logic(availableAi("наткнуться"), repo)
        logic.suggest(term = "come across")

        logic.confirmSelected()

        assertTrue(repo.upserted.isEmpty())
    }

    @Test
    fun `a manual sense keeps its surface form and unit type through confirm`() {
        val repo = FakeSenseWriteRepository()
        val logic = logic(availableAi(), repo)
        logic.suggest(term = "come across")

        logic.addManual(
            translation = "  наткнуться  ",
            surfaceForm = "come across [as]",
            unitType = GrammarUnitType.PhrasalVerb,
        )
        logic.confirmSelected()

        val confirmed = repo.upserted.single()
        assertEquals("наткнуться", confirmed.translation)
        assertEquals(GrammarUnitType.PhrasalVerb, confirmed.unitType)
        assertNotNull(confirmed.surfaceForm)
    }

    @Test
    fun `toggling a manual suggestion off clears the selection`() {
        val logic = logic(availableAi("enriched sense"))
        logic.suggest(term = "come across")
        logic.addManual("мой черновик")
        logic.completeManualWithAssistant(manualIndex = 0)

        val suggestionKey = logic.manualSuggestionContentKey(0, 0)
        logic.toggleManualSuggestion(manualIndex = 0, suggestionContentKey = suggestionKey)
        logic.toggleManualSuggestion(manualIndex = 0, suggestionContentKey = suggestionKey)

        val sense =
            logic.uiState.value.manualSenses
                .single()
        assertTrue(sense.assistantSuggestions.none { it.selected })
    }

    @Test
    fun `a completed manual sense with no suggestion picked confirms the thin draft`() {
        val repo = FakeSenseWriteRepository()
        val logic = logic(availableAi("enriched sense"), repo)
        logic.suggest(term = "come across")
        logic.addManual("мой черновик")
        logic.completeManualWithAssistant(manualIndex = 0)

        logic.confirmSelected()

        assertEquals(listOf("мой черновик"), repo.upserted.map { it.translation })
    }

    @Test
    fun `the assistant returning no senses marks the manual sense complete-failed`() {
        val logic = logic(availableAi())
        logic.suggest(term = "come across")
        logic.addManual("мой черновик")

        logic.completeManualWithAssistant(manualIndex = 0)

        val sense =
            logic.uiState.value.manualSenses
                .single()
        assertEquals(CaptureSenseStatus.CompleteFailed, sense.status)
    }

    @Test
    fun `toggling a candidate by an unknown content key is a no-op`() {
        val logic = logic(availableAi("наткнуться"))
        logic.suggest(term = "come across")

        logic.toggleCandidate("not-a-real-candidate")

        val state = logic.uiState.value
        assertTrue(state.candidates.none { it.selected })
    }

    @Test
    fun `selection is keyed by sense identity not list position`() {
        val repo = FakeSenseWriteRepository()
        val logic = logic(availableAi("наткнуться", "произвести впечатление", "ощущаться"), repo)
        logic.suggest(term = "come across")

        // Select by the content key of the middle sense; confirm must save
        // exactly it, independent of where it sits in the candidate list.
        logic.toggleCandidate(logic.candidateContentKey(1))
        logic.confirmSelected()

        assertEquals(listOf("произвести впечатление"), repo.upserted.map { it.translation })
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
        val logic =
            logic(
                ai = availableAi("существительное"),
                labelsProvider = StaticLabelsProvider(labels),
            )
        logic.suggest(term = "thing")
        logic.toggleCandidate(logic.candidateContentKey(0))
        logic.confirmSelected()

        logic.reset()

        val state = logic.uiState.value
        assertEquals(DataLoadingState.Success, state.grammarLabelsState)
        assertEquals("Noun", state.grammarLabels.unitType(GrammarUnitType.Noun, "en", GrammarLabelForm.Long))
    }
}
