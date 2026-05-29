package app.sensee.verification.core

/**
 * Narrow sub-contracts that a single adapter implements only for the slice it
 * actually knows. Free Dictionary / Datamuse contribute [LexicalEntryLookup];
 * the Sensee-curated backend contributes [FrequencyProvider], [CefrLevelProvider],
 * [SenseInventoryProvider] and [LexicalFamilyProvider]; LanguageTool contributes
 * [ExampleQualityChecker]. The aggregator in `integration` fans out to whoever
 * can answer and stitches the results into one report.
 *
 * Each method returns a result-shaped value rather than throwing: the seam
 * never propagates exceptions, even on provider errors. An adapter that
 * cannot answer returns the `Unavailable` shape of its contract; an adapter
 * that partially answers returns `Degraded` with whatever it has.
 */
public interface LexicalEntryLookup {
    public suspend fun lookup(query: LexicalVerificationQuery): LexicalEntryLookupResult
}

public interface SenseInventoryProvider {
    public suspend fun senses(query: LexicalVerificationQuery): SenseInventoryResult
}

public interface FrequencyProvider {
    public suspend fun frequency(query: LexicalVerificationQuery): FrequencyResult
}

public interface CefrLevelProvider {
    public suspend fun cefr(query: LexicalVerificationQuery): CefrResult
}

public interface ExampleQualityChecker {
    public suspend fun check(request: ExampleCheckRequest): ExampleCheckResult
}

public interface LexicalFamilyProvider {
    public suspend fun resolveUnit(query: LexicalVerificationQuery): UnitResolutionResult

    public suspend fun family(
        lemma: LemmaId,
        cap: Int,
    ): FamilyResult
}

public data class LexicalEntryLookupResult(
    val availability: VerifierAvailability,
    val existence: LexicalExistence,
    val normalized: NormalizationOutcome,
    val entryType: LexicalEntryTypeHint? = null,
    val partsOfSpeech: List<PartOfSpeechHint> = emptyList(),
    /**
     * Pronunciation hitched onto the same lookup payload (IPA + audio URLs).
     * Adapters whose API returns pronunciation in the same round trip fill
     * this in so the aggregator can route it into the report's pronunciation
     * evidence set without a second remote call.
     */
    val pronunciation: PronunciationInfo? = null,
    val confidence: Confidence,
    val sources: List<LexicalSourceRef>,
)

public data class SenseInventoryResult(
    val availability: VerifierAvailability,
    val mapping: SenseMapping,
    val sources: List<LexicalSourceRef>,
)

public data class FrequencyResult(
    val availability: VerifierAvailability,
    val score: FrequencyScore?,
    val confidence: Confidence,
    val sources: List<LexicalSourceRef>,
)

public data class CefrResult(
    val availability: VerifierAvailability,
    val level: CefrLevel?,
    val confidence: Confidence,
    val sources: List<LexicalSourceRef>,
)

public data class ExampleCheckRequest(
    val sentence: SentenceHint,
    val studyLanguageTag: String,
    val senseHintId: String? = null,
    val policy: VerificationPolicy = VerificationPolicy.Default,
)

public data class ExampleCheckResult(
    val availability: VerifierAvailability,
    val issues: List<ExampleIssue>,
    val rewrite: SuggestedAction.RewriteExample? = null,
    val sources: List<LexicalSourceRef>,
)

public data class UnitResolutionResult(
    val availability: VerifierAvailability,
    val unit: LexicalUnitInfo?,
    val confidence: Confidence,
    val sources: List<LexicalSourceRef>,
)

public data class FamilyResult(
    val availability: VerifierAvailability,
    val lemma: LexicalLemma?,
    val units: List<LexicalUnitInfo>,
    val truncated: Boolean,
    val sources: List<LexicalSourceRef>,
)
