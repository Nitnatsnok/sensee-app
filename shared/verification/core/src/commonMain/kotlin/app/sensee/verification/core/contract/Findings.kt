package app.sensee.verification.core.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One verification observation worth surfacing to the orchestrator. The
 * [code] is a stable machine id (e.g. `POS_MISMATCH`, `SPELLING_NORMALIZED`)
 * so feature-side localization and tests can pin against it; [message] is a
 * human-readable English fallback. Severity drives surfacing: [Info] is a
 * silent annotation, [Warning] is a soft UX hint, [Error] is something the
 * orchestrator should not ignore.
 *
 * [suggestedActions] are advisory only — the seam never applies them itself
 * (see ADR-001 "output is always a candidate" and the seam invariants in
 * `shared/verification/AGENTS.md`). The orchestrator picks which to surface
 * to the user and which to apply behind the scenes.
 */
@Serializable
public data class Finding(
    val code: String,
    val severity: FindingSeverity,
    val target: FindingTarget,
    val message: String,
    val sources: List<LexicalSourceRef>,
    val suggestedActions: List<SuggestedAction> = emptyList(),
)

@Serializable
public enum class FindingSeverity {
    Info,
    Warning,
    Error,
}

@Serializable
public sealed interface FindingTarget {
    @Serializable
    @SerialName("headword")
    public data object Headword : FindingTarget

    @Serializable
    @SerialName("sense")
    public data class Sense(
        val senseHintId: String,
    ) : FindingTarget

    @Serializable
    @SerialName("example")
    public data class Example(
        val senseHintId: String?,
        val index: Int,
    ) : FindingTarget
}

@Serializable
public sealed interface SuggestedAction {
    @Serializable
    @SerialName("replaceHeadword")
    public data class ReplaceHeadword(
        val value: String,
    ) : SuggestedAction

    @Serializable
    @SerialName("changePos")
    public data class ChangePartOfSpeech(
        val pos: PartOfSpeechHint,
    ) : SuggestedAction

    @Serializable
    @SerialName("changeEntryType")
    public data class ChangeEntryType(
        val entryType: LexicalEntryTypeHint,
    ) : SuggestedAction

    @Serializable
    @SerialName("removeSense")
    public data class RemoveSense(
        val senseHintId: String,
    ) : SuggestedAction

    @Serializable
    @SerialName("addSense")
    public data class AddSense(
        val gloss: String,
        val pos: PartOfSpeechHint? = null,
    ) : SuggestedAction

    /**
     * A rewrite of an example sentence in its [SentenceHint] form so the
     * Target-segment topology is preserved when the adapter could map it. If
     * [requiresTargetReannotation] is `true` the rewrite cannot be applied
     * silently — the studied unit may have moved across segment boundaries
     * and the orchestrator must surface the conflict for manual review.
     */
    @Serializable
    @SerialName("rewriteExample")
    public data class RewriteExample(
        val draft: SentenceHint,
        val requiresTargetReannotation: Boolean = false,
    ) : SuggestedAction
}

/**
 * One issue an example-quality adapter raises against a candidate sentence
 * (returned in [ExampleCheckResult.issues]). [ExampleQualityChecker] runs as a
 * direct, post-AI call from the capture orchestrator (ADR-007); the result is
 * not folded into [LexicalVerificationReport].
 */
@Serializable
public data class ExampleIssue(
    val code: String,
    val severity: FindingSeverity,
    val location: ExampleLocation,
    val message: String,
    val sources: List<LexicalSourceRef>,
)

/**
 * Where in a structured example a finding applies. [Segment] points at a
 * specific [SentenceHint.Segment] by index — Target topology is preserved.
 * [PlainTextSpan] is a fallback for adapters that only know offsets over
 * [SentenceHint.plainText]; the orchestrator may upcast a `PlainTextSpan`
 * back to `Segment` when the range sits entirely inside one segment.
 */
@Serializable
public sealed interface ExampleLocation {
    @Serializable
    @SerialName("whole")
    public data object WholeSentence : ExampleLocation

    @Serializable
    @SerialName("segment")
    public data class Segment(
        val segmentIndex: Int,
    ) : ExampleLocation

    @Serializable
    @SerialName("withinSegment")
    public data class WithinSegment(
        val segmentIndex: Int,
        @Serializable(with = IntRangeSerializer::class)
        val range: IntRange,
    ) : ExampleLocation

    @Serializable
    @SerialName("plainTextSpan")
    public data class PlainTextSpan(
        @Serializable(with = IntRangeSerializer::class)
        val range: IntRange,
    ) : ExampleLocation
}
