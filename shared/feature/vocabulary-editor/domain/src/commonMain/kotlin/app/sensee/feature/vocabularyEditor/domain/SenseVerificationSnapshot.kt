package app.sensee.feature.vocabularyEditor.domain

/**
 * Feature-side projection of a [LexicalVerificationReport] slice that applies
 * to one [SenseCandidate]. The seam owns the full report shape; this
 * wrapper carries only the fields the capture UI may render, filtered to the
 * specific candidate (Headword findings broadcast to every candidate; Sense
 * findings ride only the matching candidate). Persisted material does not
 * carry this snapshot — it is transient evidence for the capture session.
 */
public data class SenseVerificationSnapshot(
    val existence: SenseExistenceTag,
    val entryType: String? = null,
    val cefr: String? = null,
    val frequencyBand: String? = null,
    val frequencyZipf: Double? = null,
    val findings: List<SenseVerificationFinding> = emptyList(),
    val family: SenseFamilyProjection? = null,
    val sources: List<SenseVerificationSource> = emptyList(),
    val availability: SenseVerificationAvailability = SenseVerificationAvailability.Available,
)

public enum class SenseExistenceTag {
    /** A verifier source confirmed the entry. */
    Confirmed,

    /** A verifier source asserts the entry does not exist. */
    NotFound,

    /** No source had an opinion either way; treat the field as silent. */
    Unverified,
}

public enum class SenseVerificationAvailability {
    Available,
    Degraded,
    Unavailable,
}

public data class SenseVerificationFinding(
    val code: String,
    val severity: SenseVerificationSeverity,
    val scope: SenseVerificationScope,
    val message: String,
)

public enum class SenseVerificationSeverity {
    Info,
    Warning,
    Error,
}

public sealed interface SenseVerificationScope {
    /** Applies to the whole entry — shown on every sense card. */
    public data object Headword : SenseVerificationScope

    /** Applies specifically to the sense carrying this snapshot. */
    public data object ThisSense : SenseVerificationScope

    /** Applies to one example of the sense; [exampleIndex] keys the example. */
    public data class Example(
        val exampleIndex: Int,
    ) : SenseVerificationScope
}

public data class SenseFamilyProjection(
    val headLemmaCanonical: String,
    val siblings: List<SenseFamilySibling>,
    val truncated: Boolean = false,
)

public data class SenseFamilySibling(
    val displayForm: String,
    val entryType: String,
)

public data class SenseVerificationSource(
    val id: String,
    val displayName: String,
)
