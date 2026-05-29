package app.sensee.verification.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Outcome of one [LexicalVerifier.verify] call. Per-claim values are wrapped
 * in [EvidenceSet] so two providers that disagree are surfaced as a conflict
 * with attribution, not silently last-writer-wins. Per-source evidence and a
 * separate [findings] stream let the orchestrator decide what to apply,
 * surface as a warning, or ignore.
 *
 * Invariant: [VerifierAvailability.Unavailable] implies all evidence sets are
 * empty, [findings] are empty, and [family] is `null`.
 */
@Serializable
public data class LexicalVerificationReport(
    val availability: VerifierAvailability,
    val normalized: NormalizationOutcome = NormalizationOutcome.EMPTY,
    val existence: EvidenceSet<LexicalExistence> = EvidenceSet.empty(),
    val entryType: EvidenceSet<LexicalEntryTypeHint?> = EvidenceSet.empty(),
    val partsOfSpeech: EvidenceSet<List<PartOfSpeechHint>> = EvidenceSet.empty(),
    val senseMapping: SenseMapping = SenseMapping.EMPTY,
    val cefr: EvidenceSet<CefrLevel?> = EvidenceSet.empty(),
    val frequency: EvidenceSet<FrequencyScore?> = EvidenceSet.empty(),
    val pronunciation: EvidenceSet<PronunciationInfo?> = EvidenceSet.empty(),
    val family: FamilyContext? = null,
    val exampleFindings: List<ExampleFinding> = emptyList(),
    val findings: List<Finding> = emptyList(),
    val sources: List<LexicalSource> = emptyList(),
) {
    init {
        if (availability is VerifierAvailability.Unavailable) {
            val carriesEvidence =
                existence.observations.isNotEmpty() ||
                    entryType.observations.isNotEmpty() ||
                    partsOfSpeech.observations.isNotEmpty() ||
                    cefr.observations.isNotEmpty() ||
                    frequency.observations.isNotEmpty() ||
                    pronunciation.observations.isNotEmpty() ||
                    senseMapping != SenseMapping.EMPTY ||
                    normalized != NormalizationOutcome.EMPTY ||
                    exampleFindings.isNotEmpty() ||
                    findings.isNotEmpty() ||
                    family != null
            require(!carriesEvidence) { "Unavailable verification must carry no evidence, findings, or family" }
        }
    }

    public companion object {
        /**
         * An empty report. Empty [missingProviders] yields
         * [VerifierAvailability.Unavailable]; a non-empty list yields
         * [VerifierAvailability.Degraded] naming the providers that dropped out.
         */
        public fun unavailable(
            reason: String,
            missingProviders: List<String> = emptyList(),
        ): LexicalVerificationReport =
            LexicalVerificationReport(
                availability =
                    if (missingProviders.isEmpty()) {
                        VerifierAvailability.Unavailable(reason)
                    } else {
                        VerifierAvailability.Degraded(reason, missingProviders)
                    },
            )
    }
}

@Serializable
public sealed interface VerifierAvailability {
    @Serializable
    @SerialName("available")
    public data object Available : VerifierAvailability

    @Serializable
    @SerialName("degraded")
    public data class Degraded(
        val reason: String,
        val missingProviders: List<String> = emptyList(),
    ) : VerifierAvailability

    @Serializable
    @SerialName("unavailable")
    public data class Unavailable(
        val reason: String,
    ) : VerifierAvailability
}

/**
 * Tri-state existence so "the verifier could not check" never collapses into
 * "the unit does not exist". Only [NotFound] gates downstream decisions about
 * spelling; [Unknown] tells the UI to stay quiet rather than alarm the user.
 */
@Serializable
public enum class LexicalExistence {
    Confirmed,
    NotFound,
    Unknown,
}
