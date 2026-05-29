package app.sensee.feature.vocabularyEditor.domain

import app.sensee.ai.core.AiEnrichmentClient
import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.core.SenseCoverage
import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import app.sensee.verification.core.LexicalVerificationQuery
import app.sensee.verification.core.LexicalVerificationReport
import app.sensee.verification.core.LexicalVerifier
import app.sensee.verification.core.VerificationPolicy
import dev.zacsweers.metro.Inject

/**
 * Capture-suggestion orchestration: ensure a single draft per capture session,
 * enrich the term through the AI seam, map the result into structured
 * candidates. UI copy (availability note, loading state) stays in presentation.
 */
@Inject
public class SuggestVocabularySensesUseCase(
    private val vocabularyRepository: VocabularyRepository,
    private val enrichmentClient: AiEnrichmentClient,
    private val verifier: LexicalVerifier,
    private val taxonomyInvariantsProvider: TaxonomyInvariantsProvider,
    private val appDiagnostics: AppDiagnostics,
) {
    public data class Suggestion(
        val draftId: EntryId,
        val candidates: List<SenseCandidate>,
        val availability: EnrichmentAvailability,
    )

    public data class CandidateSuggestion(
        val candidates: List<SenseCandidate>,
        val availability: EnrichmentAvailability,
    )

    /**
     * [existingDraftId] reuses the session's draft, so re-running suggest
     * (re-entry, an edited term) never spawns an orphan row. [term] is expected
     * already trimmed (input sanitation is a presentation concern).
     */
    public suspend operator fun invoke(
        term: String,
        existingDraftId: EntryId?,
        studyLanguageTag: String = "en",
        nativeLanguageTag: String = "ru",
    ): Suggestion {
        // One draft per capture session; edited input updates that draft
        // instead of spawning an orphan row or confirming stale text.
        val draft =
            existingDraftId
                ?.let { vocabularyRepository.updateDraftTerm(it, term) }
                ?: vocabularyRepository.createDraft(term)
        val draftId = draft.id
        // Evidence-augmented pass: a verifier consulted before the AI gets a
        // chance to commit to senses gives the model a factual anchor (lemma,
        // entry type, CEFR, frequency, family). License-gated inside the
        // builder — only sources whose policy allows third-party-LLM context
        // contribute; an unavailable or restricted verifier collapses to
        // `null` and the AI behaves exactly as before.
        val result = suggestCandidates(term, studyLanguageTag, nativeLanguageTag)
        return Suggestion(
            draftId = draftId,
            candidates = result.candidates,
            availability = result.availability,
        )
    }

    /**
     * Enriches an already owned entry without touching the draft lifecycle.
     * Used by edit mode: the user is replacing accepted content, not opening a
     * new capture session.
     */
    public suspend fun suggestCandidates(
        term: String,
        studyLanguageTag: String = "en",
        nativeLanguageTag: String = "ru",
    ): CandidateSuggestion {
        val result =
            enrichWithEvidence(
                term = term,
                studyLanguageTag = studyLanguageTag,
                nativeLanguageTag = nativeLanguageTag,
            )
        return CandidateSuggestion(
            candidates = mapResultToCandidates(result = result, fallbackTerm = term),
            availability = result.availability,
        )
    }

    /**
     * Enrich a user-authored sense with the same pre-AI verifier evidence used
     * by the main suggestion path. The user's text is a disambiguating hint;
     * [SenseCoverage.Minimal] asks the provider for only that described sense.
     */
    public suspend fun completeManualSense(
        term: String,
        userNote: String,
        studyLanguageTag: String = "en",
        nativeLanguageTag: String = "ru",
    ): List<SenseCandidate> =
        mapResultToCandidates(
            result =
                enrichWithEvidence(
                    term = term,
                    studyLanguageTag = studyLanguageTag,
                    nativeLanguageTag = nativeLanguageTag,
                    userNote = userNote,
                    senseCoverage = SenseCoverage.Minimal,
                ),
            fallbackTerm = term,
        )

    private suspend fun enrichWithEvidence(
        term: String,
        studyLanguageTag: String,
        nativeLanguageTag: String,
        userNote: String? = null,
        senseCoverage: SenseCoverage = SenseCoverage.Common,
    ): EnrichmentResult {
        val evidence = gatherEvidence(term, studyLanguageTag = studyLanguageTag)
        return enrichmentClient.enrich(
            EnrichmentRequest(
                term = term,
                studyLanguageTag = studyLanguageTag,
                nativeLanguageTag = nativeLanguageTag,
                userNote = userNote,
                senseCoverage = senseCoverage,
                evidence = evidence,
            ),
        )
    }

    private suspend fun mapResultToCandidates(
        result: EnrichmentResult,
        fallbackTerm: String,
    ): List<SenseCandidate> =
        result.toSenseCandidates(
            fallbackTerm = fallbackTerm,
            invariants = taxonomyInvariantsProvider.invariants(),
            onUnknown = appDiagnostics::warnUnknownTaxonomyValue,
        )

    private suspend fun gatherEvidence(
        term: String,
        studyLanguageTag: String,
    ): app.sensee.ai.core.EnrichmentEvidence? {
        val report = runVerifier(term, studyLanguageTag) ?: return null
        return report.toEnrichmentEvidence()
    }

    private suspend fun runVerifier(
        term: String,
        studyLanguageTag: String,
    ): LexicalVerificationReport? =
        runCatchingCancellable {
            verifier.verify(
                LexicalVerificationQuery(
                    text = term,
                    studyLanguageTag = studyLanguageTag,
                    policy = VerificationPolicy(includeFamily = true),
                ),
            )
        }.getOrElse { throwable ->
            // A failing verifier never blocks the suggestion path — capture
            // keeps working uninstrumented.
            appDiagnostics.logger.tag("SuggestEvidence").warn(throwable) {
                "Verifier raised while gathering evidence; enrichment proceeds without it"
            }
            null
        }
}
