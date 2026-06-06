package app.sensee.feature.vocabularyEditor.domain.usecase

import app.sensee.ai.core.contract.AiEnrichmentClient
import app.sensee.ai.core.contract.EnrichmentAvailability
import app.sensee.ai.core.contract.EnrichmentResult
import app.sensee.ai.core.request.EnrichmentRequest
import app.sensee.ai.core.request.SenseCoverage
import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.feature.vocabularyEditor.domain.mapping.toEnrichmentGrounding
import app.sensee.grammar.domain.SentenceSegment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.deriveSenseContentKey
import app.sensee.lexicon.domain.isConfirmable
import app.sensee.lexicon.enrichment.toSenses
import app.sensee.verification.core.contract.ExampleCheckRequest
import app.sensee.verification.core.contract.ExampleQualityChecker
import app.sensee.verification.core.contract.FindingSeverity
import app.sensee.verification.core.contract.LexicalVerificationQuery
import app.sensee.verification.core.contract.LexicalVerificationReport
import app.sensee.verification.core.contract.LexicalVerifier
import app.sensee.verification.core.contract.SentenceHint
import app.sensee.verification.core.contract.VerificationPolicy
import app.sensee.verification.core.contract.VerifierAvailability
import dev.zacsweers.metro.Inject

/**
 * Capture-suggestion orchestration: enrich the term through the AI seam and map
 * the result into in-progress [Sense]s. The mapper is the only seam producing a
 * [Sense] (ADR-001); a suggestion is just an unsaved sense the user may confirm,
 * persisted on confirm through the sense store, not here. UI copy (availability
 * note, loading state) stays in presentation.
 */
@Inject
public class SuggestVocabularySensesUseCase(
    private val enrichmentClient: AiEnrichmentClient,
    private val verifier: LexicalVerifier,
    private val exampleQualityCheckers: Set<ExampleQualityChecker>,
    private val taxonomyInvariantsProvider: TaxonomyInvariantsProvider,
    private val appDiagnostics: AppDiagnostics,
) {
    public data class SenseSuggestion(
        val senses: List<Sense>,
        val availability: EnrichmentAvailability,
    )

    /**
     * Enrich [term] and map the result into in-progress senses. [term] is
     * expected already trimmed (input sanitation is a presentation concern).
     */
    public suspend operator fun invoke(
        term: String,
        studyLanguageTag: String = "en",
        nativeLanguageTag: String = "ru",
    ): SenseSuggestion {
        val result =
            enrichWithGrounding(
                term = term,
                studyLanguageTag = studyLanguageTag,
                nativeLanguageTag = nativeLanguageTag,
            )
        return SenseSuggestion(
            senses = mapResultToSenses(result = result, fallbackTerm = term, studyLanguageTag = studyLanguageTag),
            availability = result.availability,
        )
    }

    /**
     * Enrich a user-authored sense with the same pre-AI verifier grounding used
     * by the main suggestion path. The user's text is a disambiguating hint;
     * [SenseCoverage.Minimal] asks the provider for only that described sense.
     */
    public suspend fun completeManualSense(
        term: String,
        userNote: String,
        studyLanguageTag: String = "en",
        nativeLanguageTag: String = "ru",
    ): List<Sense> =
        mapResultToSenses(
            result =
                enrichWithGrounding(
                    term = term,
                    studyLanguageTag = studyLanguageTag,
                    nativeLanguageTag = nativeLanguageTag,
                    userNote = userNote,
                    senseCoverage = SenseCoverage.Minimal,
                ),
            fallbackTerm = term,
            studyLanguageTag = studyLanguageTag,
        )

    private suspend fun enrichWithGrounding(
        term: String,
        studyLanguageTag: String,
        nativeLanguageTag: String,
        userNote: String? = null,
        senseCoverage: SenseCoverage = SenseCoverage.Common,
    ): EnrichmentResult {
        val grounding = gatherGrounding(term, studyLanguageTag = studyLanguageTag)
        return enrichmentClient.enrich(
            EnrichmentRequest(
                term = term,
                studyLanguageTag = studyLanguageTag,
                nativeLanguageTag = nativeLanguageTag,
                userNote = userNote,
                senseCoverage = senseCoverage,
                grounding = grounding,
            ),
        )
    }

    // Distinct content keys keep one card per sense: two suggestions that share
    // a content key are the same sense (selection is keyed by content key), so
    // the duplicate would otherwise be unselectable. Each kept sense then passes
    // the silent post-AI example-quality filter, and any sense the provider left
    // without a usable example (or translation) is dropped here — never surfaced
    // as a candidate the user could select but the confirm-gate would reject.
    private suspend fun mapResultToSenses(
        result: EnrichmentResult,
        fallbackTerm: String,
        studyLanguageTag: String,
    ): List<Sense> =
        result
            .toSenses(
                fallbackTerm = fallbackTerm,
                invariants = taxonomyInvariantsProvider.invariants(),
                onUnknown = appDiagnostics::warnUnknownTaxonomyValue,
            ).distinctBy(::deriveSenseContentKey)
            .map { filterWeakExamples(it, studyLanguageTag) }
            .filter { it.isConfirmable() }

    // Post-AI example-quality (ADR-007), the verifier's second narrow role: one
    // optional, silent ExampleQualityChecker call per example before save. Drops
    // an example only when the checker flags an Error, and NEVER empties the list
    // — a confirmed sense keeps at least one example (a model invariant). A
    // failing or Unavailable checker leaves the examples untouched (non-blocking).
    private suspend fun filterWeakExamples(
        sense: Sense,
        studyLanguageTag: String,
    ): Sense {
        val examples = sense.contextualApplications
        if (examples.size <= 1) return sense
        val kept = examples.filterNot { hasBlockingIssue(it.sentence, studyLanguageTag) }
        val retained = kept.ifEmpty { listOf(examples.first()) }
        return if (retained.size == examples.size) sense else sense.copy(contextualApplications = retained)
    }

    private suspend fun hasBlockingIssue(
        sentence: StudiedSentence,
        studyLanguageTag: String,
    ): Boolean {
        // One optional checker, no multi-channel fan-out: the first wired
        // checker, or none → nothing flagged, nothing dropped.
        val checker = exampleQualityCheckers.firstOrNull() ?: return false
        val check =
            runCatchingCancellable {
                checker.check(
                    ExampleCheckRequest(
                        sentence = sentence.toVerificationHint(),
                        studyLanguageTag = studyLanguageTag,
                    ),
                )
            }.getOrElse { throwable ->
                appDiagnostics.logger.tag("ExampleQuality").warn(throwable) {
                    "Example-quality checker raised; example kept as-is"
                }
                return false
            }
        if (check.availability is VerifierAvailability.Unavailable) return false
        return check.issues.any { it.severity == FindingSeverity.Error }
    }

    private suspend fun gatherGrounding(
        term: String,
        studyLanguageTag: String,
    ): app.sensee.ai.core.grounding.EnrichmentGrounding? {
        val report = runVerifier(term, studyLanguageTag) ?: return null
        return report.toEnrichmentGrounding()
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
            appDiagnostics.logger.tag("SuggestGrounding").warn(throwable) {
                "Verifier raised while gathering grounding; enrichment proceeds without it"
            }
            null
        }
}

private fun StudiedSentence.toVerificationHint(): SentenceHint =
    SentenceHint(
        segments =
            segments.map { segment ->
                when (segment) {
                    is SentenceSegment.Text -> SentenceHint.Segment.Text(segment.value)
                    is SentenceSegment.Target -> SentenceHint.Segment.Target(segment.value)
                }
            },
    )
