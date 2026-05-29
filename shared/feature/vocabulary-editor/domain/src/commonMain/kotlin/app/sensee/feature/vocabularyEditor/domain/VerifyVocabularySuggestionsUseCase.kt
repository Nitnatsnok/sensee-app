package app.sensee.feature.vocabularyEditor.domain

import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.grammar.domain.SentenceSegment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.verification.core.ExampleHint
import app.sensee.verification.core.FamilyContext
import app.sensee.verification.core.Finding
import app.sensee.verification.core.FindingSeverity
import app.sensee.verification.core.FindingTarget
import app.sensee.verification.core.FrequencyScore
import app.sensee.verification.core.LexicalExistence
import app.sensee.verification.core.LexicalSource
import app.sensee.verification.core.LexicalVerificationQuery
import app.sensee.verification.core.LexicalVerificationReport
import app.sensee.verification.core.LexicalVerifier
import app.sensee.verification.core.SenseHint
import app.sensee.verification.core.SentenceHint
import app.sensee.verification.core.VerificationPolicy
import app.sensee.verification.core.VerifierAvailability
import dev.zacsweers.metro.Inject

/**
 * Runs the verifier across the AI-proposed candidates and attaches a
 * per-candidate [SenseVerificationSnapshot] without rewriting any AI
 * material (ADR-001: the seam never silently changes data). Headword-level
 * facts (existence, CEFR, frequency, family) ride on every candidate;
 * findings are filtered by [FindingTarget] so a `Sense` finding lands only
 * on the sense it identifies. A degraded or unavailable verifier leaves the
 * candidates untouched — uninstrumented is the safe fallback.
 */
@Inject
public class VerifyVocabularySuggestionsUseCase(
    private val verifier: LexicalVerifier,
    private val appDiagnostics: AppDiagnostics,
) {
    public suspend operator fun invoke(
        term: String,
        studyLanguageTag: String,
        candidates: List<SenseCandidate>,
    ): List<SenseCandidate> {
        if (term.isBlank() || candidates.isEmpty()) return candidates
        val report = runVerifier(term, studyLanguageTag, candidates) ?: return candidates
        // Unavailable verifier → no snapshot: a null `verification` lets the UI
        // tell "we never asked" apart from "we asked and the verifier had a
        // partial answer". Degraded still carries through so the partial
        // evidence is renderable.
        if (report.availability is VerifierAvailability.Unavailable) return candidates
        val exampleIndexLookup = candidates.exampleIndexLookup()
        return candidates.map { candidate ->
            candidate.copy(
                verification =
                    report.snapshotFor(
                        candidate = candidate,
                        exampleIndexLookup = exampleIndexLookup[candidate.id].orEmpty(),
                    ),
            )
        }
    }

    private suspend fun runVerifier(
        term: String,
        studyLanguageTag: String,
        candidates: List<SenseCandidate>,
    ): LexicalVerificationReport? =
        runCatchingCancellable {
            verifier.verify(query(term, studyLanguageTag, candidates))
        }.getOrElse { throwable ->
            // Never propagate across the use case boundary — the capture flow
            // must keep working even if a verifier adapter raises.
            appDiagnostics.logger.tag("VerifySenses").warn(throwable) {
                "Verifier raised; capture continues without verification snapshots"
            }
            null
        }

    private fun query(
        term: String,
        studyLanguageTag: String,
        candidates: List<SenseCandidate>,
    ): LexicalVerificationQuery =
        LexicalVerificationQuery(
            text = term,
            studyLanguageTag = studyLanguageTag,
            senseHints =
                candidates.map { candidate ->
                    SenseHint(
                        id = candidate.id.value,
                        definition = candidate.explanation,
                        translation = candidate.translation,
                        pos = candidate.unitType?.toPartOfSpeechHint(),
                    )
                },
            examplesToValidate = candidates.toExampleHints(),
            expectedEntryType = candidates.expectedEntryTypeHint(),
            expectedPartOfSpeech = candidates.expectedPartOfSpeechHint(),
            policy = VerificationPolicy(includeFamily = true),
        )
}

private fun LexicalVerificationReport.snapshotFor(
    candidate: SenseCandidate,
    exampleIndexLookup: Map<Int, Int>,
): SenseVerificationSnapshot {
    val findings = relevantFindings(candidate)
    val exampleFindings = relevantExampleFindings(candidate, exampleIndexLookup)
    return SenseVerificationSnapshot(
        existence = existence.consensus.toFeatureTag(),
        entryType = entryType.consensus?.id,
        cefr = cefr.consensus?.name,
        frequencyBand = frequency.consensus?.band?.name,
        frequencyZipf = frequency.consensus?.let(FrequencyScore::zipf),
        findings = findings + exampleFindings,
        family = family?.toFeatureProjection(),
        sources = sources.map { it.toFeatureSource() },
        availability = availability.toFeatureAvailability(),
    )
}

private fun LexicalVerificationReport.relevantFindings(candidate: SenseCandidate): List<SenseVerificationFinding> =
    findings.mapNotNull { finding ->
        val scope =
            when (val target = finding.target) {
                FindingTarget.Headword -> SenseVerificationScope.Headword
                is FindingTarget.Sense ->
                    if (target.senseHintId == candidate.id.value) {
                        SenseVerificationScope.ThisSense
                    } else {
                        return@mapNotNull null
                    }
                is FindingTarget.Example -> return@mapNotNull null
            }
        finding.toFeatureFinding(scope)
    }

private fun LexicalVerificationReport.relevantExampleFindings(
    candidate: SenseCandidate,
    exampleIndexLookup: Map<Int, Int>,
): List<SenseVerificationFinding> =
    exampleFindings
        .mapNotNull { exampleFinding ->
            val localExampleIndex = exampleFinding.localExampleIndexFor(candidate, exampleIndexLookup)
            localExampleIndex?.let { exampleFinding to it }
        }.flatMap { (exampleFinding, localExampleIndex) ->
            exampleFinding.issues.map { issue ->
                SenseVerificationFinding(
                    code = issue.code,
                    severity = issue.severity.toFeatureSeverity(),
                    scope = SenseVerificationScope.Example(localExampleIndex),
                    message = issue.message,
                )
            }
        }

private fun List<SenseCandidate>.toExampleHints(): List<ExampleHint> =
    flatMap { candidate ->
        candidate.contextualApplications.map { application ->
            ExampleHint(
                sentence = application.sentence.toVerificationHint(),
                senseHintId = candidate.id.value,
            )
        }
    }

private fun List<SenseCandidate>.exampleIndexLookup(): Map<SenseCandidateId, Map<Int, Int>> {
    var globalIndex = 0
    return associate { candidate ->
        val candidateIndices =
            candidate.contextualApplications.indices.associate { localIndex ->
                val currentGlobalIndex = globalIndex
                globalIndex += 1
                currentGlobalIndex to localIndex
            }
        candidate.id to candidateIndices
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

private fun app.sensee.verification.core.ExampleFinding.localExampleIndexFor(
    candidate: SenseCandidate,
    exampleIndexLookup: Map<Int, Int>,
): Int? =
    when {
        // A finding either targets this candidate (its sense id) or is untagged
        // (applies to whichever candidate owns the example); in both cases its
        // global `exampleIndex` is mapped to this candidate's local index via
        // `exampleIndexLookup`. A global index outside this candidate's range
        // is dropped (null), never mis-attributed to a local example. A finding
        // tagged with a different candidate's sense id is not ours.
        senseHintId == null || senseHintId == candidate.id.value -> exampleIndexLookup[exampleIndex]
        else -> null
    }

private fun Finding.toFeatureFinding(scope: SenseVerificationScope): SenseVerificationFinding =
    SenseVerificationFinding(
        code = code,
        severity = severity.toFeatureSeverity(),
        scope = scope,
        message = message,
    )

private fun FindingSeverity.toFeatureSeverity(): SenseVerificationSeverity =
    when (this) {
        FindingSeverity.Info -> SenseVerificationSeverity.Info
        FindingSeverity.Warning -> SenseVerificationSeverity.Warning
        FindingSeverity.Error -> SenseVerificationSeverity.Error
    }

private fun LexicalExistence?.toFeatureTag(): SenseExistenceTag =
    when (this) {
        LexicalExistence.Confirmed -> SenseExistenceTag.Confirmed
        LexicalExistence.NotFound -> SenseExistenceTag.NotFound
        LexicalExistence.Unknown, null -> SenseExistenceTag.Unverified
    }

private fun FamilyContext.toFeatureProjection(): SenseFamilyProjection? {
    val head = headLemma ?: return null
    return SenseFamilyProjection(
        headLemmaCanonical = head.canonical,
        siblings =
            siblings.map { sibling ->
                SenseFamilySibling(
                    displayForm = sibling.displayForm,
                    entryType = sibling.entryType.id,
                )
            },
        truncated = truncated,
    )
}

private fun LexicalSource.toFeatureSource(): SenseVerificationSource =
    SenseVerificationSource(id = id, displayName = displayName)

private fun VerifierAvailability.toFeatureAvailability(): SenseVerificationAvailability =
    when (this) {
        VerifierAvailability.Available -> SenseVerificationAvailability.Available
        is VerifierAvailability.Degraded -> SenseVerificationAvailability.Degraded
        is VerifierAvailability.Unavailable -> SenseVerificationAvailability.Unavailable
    }
