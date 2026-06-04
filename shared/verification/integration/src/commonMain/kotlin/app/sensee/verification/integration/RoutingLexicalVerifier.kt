package app.sensee.verification.integration

import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.logging.AppLogger
import app.sensee.verification.core.contract.AttributionPolicy
import app.sensee.verification.core.contract.CefrLevelProvider
import app.sensee.verification.core.contract.CefrResult
import app.sensee.verification.core.contract.Confidence
import app.sensee.verification.core.contract.EvidenceSet
import app.sensee.verification.core.contract.FamilyResult
import app.sensee.verification.core.contract.FrequencyProvider
import app.sensee.verification.core.contract.FrequencyResult
import app.sensee.verification.core.contract.LexicalEntryLookup
import app.sensee.verification.core.contract.LexicalEntryLookupResult
import app.sensee.verification.core.contract.LexicalEntryTypeHint
import app.sensee.verification.core.contract.LexicalExistence
import app.sensee.verification.core.contract.LexicalFamilyProvider
import app.sensee.verification.core.contract.LexicalSource
import app.sensee.verification.core.contract.LexicalVerificationQuery
import app.sensee.verification.core.contract.LexicalVerificationReport
import app.sensee.verification.core.contract.LexicalVerifier
import app.sensee.verification.core.contract.LicensePolicy
import app.sensee.verification.core.contract.NormalizationCandidate
import app.sensee.verification.core.contract.NormalizationOutcome
import app.sensee.verification.core.contract.Observation
import app.sensee.verification.core.contract.PartOfSpeechHint
import app.sensee.verification.core.contract.SenseInventoryProvider
import app.sensee.verification.core.contract.SenseInventoryResult
import app.sensee.verification.core.contract.VerifierAvailability
import app.sensee.verification.core.grounding.CefrLevel
import app.sensee.verification.core.grounding.DictionarySenseSummary
import app.sensee.verification.core.grounding.FrequencyScore
import app.sensee.verification.core.grounding.PronunciationInfo
import app.sensee.verification.core.grounding.SenseMapping
import app.sensee.verification.core.hierarchy.FamilyContext
import app.sensee.verification.core.hierarchy.LexicalUnitInfo
import app.sensee.verification.core.hierarchy.LexicalUnitSummary
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The umbrella verifier that fans out across every known source and merges
 * their evidence into one report. Every adapter contributes through the same
 * sub-contract Set bindings; no "source of shape" special case lives in the
 * routing class. The merge appends observations rather than picking one source
 * so disagreement surfaces as `EvidenceSet.hasConflict`, never silently
 * last-writer-wins.
 *
 * Post-AI example quality is NOT part of this fan-out: it runs as a direct,
 * single-checker call in `SuggestVocabularySensesUseCase` (ADR-007). A failing
 * adapter on any leg degrades only its own contribution and never propagates an
 * exception. The per-provider timeout in
 * [VerificationPolicy.timeoutPerProviderMillis] stops a hung adapter from
 * blocking the entire report.
 *
 * Not directly bound to [LexicalVerifier] — [PersistedCachingLexicalVerifier]
 * decorates this one and is the bound implementation.
 */
@SingleIn(AppScope::class)
@Inject
public class RoutingLexicalVerifier(
    private val contributors: VerificationContributors,
    internal val sourceCatalog: Set<LexicalSource>,
    appDiagnostics: AppDiagnostics,
) : LexicalVerifier {
    private val logger = appDiagnostics.logger.tag("LexicalVerifier")
    private val sourceCatalogById: Map<String, LexicalSource> = sourceCatalog.associateBy { it.id }

    override suspend fun verify(query: LexicalVerificationQuery): LexicalVerificationReport =
        coroutineScope {
            logger.debug {
                "Verification fan-out started: ${VerificationLogSummaries.query(query)}; " +
                    "contributors(lookups=${contributors.entryLookups.size}, " +
                    "frequencies=${contributors.frequencyProviders.size}, cefr=${contributors.cefrProviders.size}, " +
                    "senses=${contributors.senseInventoryProviders.size}, " +
                    "families=${contributors.familyProviders.size})"
            }
            val lookupDeferreds =
                contributors.entryLookups.map { lookup -> async { runLookup(lookup, query) } }
            val frequencyDeferreds =
                contributors.frequencyProviders.map { provider ->
                    async { runFrequency(provider, query) }
                }
            val cefrDeferreds =
                contributors.cefrProviders.map { provider ->
                    async { runCefr(provider, query) }
                }
            val senseDeferreds =
                contributors.senseInventoryProviders.map { provider ->
                    async { runSenseInventory(provider, query, logger) }
                }
            val familyDeferreds = launchFamilyResolution(query)
            val baseReport = LexicalVerificationReport.unavailable("no verification contributors returned evidence")
            val contributions =
                AdapterContributions(
                    lookups = lookupDeferreds.awaitAll(),
                    frequencies = frequencyDeferreds.awaitAll(),
                    cefrs = cefrDeferreds.awaitAll(),
                    senses = senseDeferreds.awaitAll(),
                    familyResolutions = familyDeferreds.awaitAll(),
                )
            val report = mergeReports(baseReport, contributions)
            logger.debug {
                "Verification fan-out finished: ${VerificationLogSummaries.report(report)}; " +
                    VerificationLogSummaries.contributions(contributions)
            }
            report
        }

    private fun CoroutineScope.launchFamilyResolution(
        query: LexicalVerificationQuery,
    ): List<Deferred<FamilyResolution>> {
        if (!query.policy.includeFamily) return emptyList()
        return contributors.familyProviders.map { provider ->
            async { runFamilyResolution(provider, query) }
        }
    }

    private suspend fun runFamilyResolution(
        provider: LexicalFamilyProvider,
        query: LexicalVerificationQuery,
    ): FamilyResolution {
        val timeoutMillis = query.policy.timeoutPerProviderMillis
        return runCatchingCancellable {
            val resolution =
                withTimeoutOrNull(timeoutMillis) { provider.resolveUnit(query) }
            if (resolution == null) {
                logger.warn {
                    "Family provider timed out: provider=${VerificationLogSummaries.providerName(provider)}, " +
                        "leg=resolveUnit, " +
                        "timeoutMs=$timeoutMillis"
                }
                return degradedFamily("resolveUnit timed out after ${timeoutMillis}ms")
            }
            val familyResult =
                resolution.unit?.headLemma?.let { lemma ->
                    val result =
                        withTimeoutOrNull(timeoutMillis) {
                            provider.family(lemma, cap = query.policy.familySiblingCap)
                        }
                    if (result == null) {
                        logger.warn {
                            "Family provider timed out: provider=${VerificationLogSummaries.providerName(provider)}, " +
                                "leg=family, " +
                                "timeoutMs=$timeoutMillis, cap=${query.policy.familySiblingCap}"
                        }
                    }
                    result
                }
            FamilyResolution(
                // Roll the two legs together — if either succeeded, count the
                // resolution as Available so the merge picks it up. A Degraded
                // resolveUnit followed by an Available family() (the lemma was
                // recognised even if the unit shape was partial) would
                // otherwise be filtered out by mergeFamily's Available-only
                // guard.
                availability = bestAvailability(resolution.availability, familyResult?.availability),
                unit = resolution.unit,
                confidence = resolution.confidence,
                family = familyResult,
            )
        }.getOrElse { throwable ->
            logger.warn(throwable) { "Family provider raised; report carries no family from it" }
            degradedFamily("adapter raised: ${throwable.message ?: throwable::class.simpleName}")
        }
    }

    private suspend fun runLookup(
        lookup: LexicalEntryLookup,
        query: LexicalVerificationQuery,
    ): LexicalEntryLookupResult {
        val timeoutMillis = query.policy.timeoutPerProviderMillis
        return runCatchingCancellable {
            val result = withTimeoutOrNull(timeoutMillis) { lookup.lookup(query) }
            if (result == null) {
                logger.warn {
                    "Entry lookup adapter timed out: provider=${VerificationLogSummaries.providerName(lookup)}, " +
                        "timeoutMs=$timeoutMillis"
                }
                degradedLookup("provider timed out after ${timeoutMillis}ms")
            } else {
                result
            }
        }.getOrElse { throwable ->
            logger.warn(throwable) { "Entry lookup adapter raised; report carries no evidence from it" }
            degradedLookup("adapter raised: ${throwable.message ?: throwable::class.simpleName}")
        }
    }

    private suspend fun runFrequency(
        provider: FrequencyProvider,
        query: LexicalVerificationQuery,
    ): FrequencyResult {
        val timeoutMillis = query.policy.timeoutPerProviderMillis
        return runCatchingCancellable {
            val result = withTimeoutOrNull(timeoutMillis) { provider.frequency(query) }
            if (result == null) {
                logger.warn {
                    "Frequency adapter timed out: provider=${VerificationLogSummaries.providerName(provider)}, " +
                        "timeoutMs=$timeoutMillis"
                }
                degradedFrequency("provider timed out after ${timeoutMillis}ms")
            } else {
                result
            }
        }.getOrElse { throwable ->
            logger.warn(throwable) { "Frequency adapter raised; report carries no evidence from it" }
            degradedFrequency("adapter raised: ${throwable.message ?: throwable::class.simpleName}")
        }
    }

    private suspend fun runCefr(
        provider: CefrLevelProvider,
        query: LexicalVerificationQuery,
    ): CefrResult {
        val timeoutMillis = query.policy.timeoutPerProviderMillis
        return runCatchingCancellable {
            val result = withTimeoutOrNull(timeoutMillis) { provider.cefr(query) }
            if (result == null) {
                logger.warn {
                    "CEFR adapter timed out: provider=${VerificationLogSummaries.providerName(provider)}, " +
                        "timeoutMs=$timeoutMillis"
                }
                degradedCefr("provider timed out after ${timeoutMillis}ms")
            } else {
                result
            }
        }.getOrElse { throwable ->
            logger.warn(throwable) { "CEFR adapter raised; report carries no evidence from it" }
            degradedCefr("adapter raised: ${throwable.message ?: throwable::class.simpleName}")
        }
    }

    private fun mergeReports(
        base: LexicalVerificationReport,
        c: AdapterContributions,
    ): LexicalVerificationReport {
        if (c.isEmpty()) return base
        val mergedAvailability =
            mergeAvailability(
                base = base.availability,
                adapter =
                    c.lookups.map { it.availability } +
                        c.frequencies.map { it.availability } +
                        c.cefrs.map { it.availability } +
                        c.senses.map { it.availability } +
                        c.familyResolutions.map { it.availability },
            )
        val mergedExistence = base.existence.append(c.lookups.mapNotNull { it.toExistenceObservation() })
        val mergedPartsOfSpeech =
            base.partsOfSpeech.append(c.lookups.mapNotNull { it.toPartsOfSpeechObservation() })
        val mergedEntryType = base.entryType.append(c.lookups.mapNotNull { it.toEntryTypeObservation() })
        val mergedFrequency =
            base.frequency.append(c.frequencies.mapNotNull { it.toFrequencyObservation() })
        val mergedCefr = base.cefr.append(c.cefrs.mapNotNull { it.toCefrObservation() })
        val mergedSenseMapping = mergeSenseMapping(base.senseMapping, c.senses)
        val mergedPronunciation =
            base.pronunciation.append(c.lookups.mapNotNull { it.toPronunciationObservation() })
        val mergedNormalization = mergeNormalization(base.normalized, c.lookups)
        val mergedFamily = mergeFamily(base.family, c.familyResolutions)
        val mergedSources = mergeSources(base.sources, c)
        return base.copy(
            availability = mergedAvailability,
            existence = mergedExistence,
            entryType = mergedEntryType,
            partsOfSpeech = mergedPartsOfSpeech,
            frequency = mergedFrequency,
            cefr = mergedCefr,
            senseMapping = mergedSenseMapping,
            pronunciation = mergedPronunciation,
            normalized = mergedNormalization,
            family = mergedFamily,
            findings = base.findings + normalizationFindings(mergedNormalization),
            sources = mergedSources,
        )
    }

    private fun mergeSources(
        baseSources: List<LexicalSource>,
        c: AdapterContributions,
    ): List<LexicalSource> {
        val knownById = baseSources.associateBy { it.id }.toMutableMap()
        val familyRefs =
            c.familyResolutions.flatMap { resolution ->
                resolution.unit?.sources.orEmpty() + resolution.family?.sources.orEmpty()
            }
        val contributedIds =
            (
                c.lookups.flatMap { it.sources } +
                    c.frequencies.flatMap { it.sources } +
                    c.cefrs.flatMap { it.sources } +
                    c.senses.flatMap { it.sources } +
                    familyRefs
            ).map { it.sourceId }.distinct()
        contributedIds.forEach { id ->
            if (id !in knownById) {
                // Fail closed on unknown provenance: a source that produced
                // evidence but is not in the catalog must still appear in the
                // report carrying the most-restrictive license, so the L2
                // persistence gate (`all { storeContentAllowed }`) cannot be
                // fooled into storing content whose license was never checked.
                knownById[id] = sourceCatalogById[id] ?: unregisteredSource(id)
            }
        }
        return knownById.values.toList()
    }

    private fun unregisteredSource(id: String): LexicalSource {
        logger.warn {
            "Verification source '$id' contributed evidence but is not registered in the source " +
                "catalog; treating it as non-persistable. Register it (see SourceCatalogCompletenessTest)."
        }
        return LexicalSource.Adapter(
            id = id,
            displayName = id,
            attribution = AttributionPolicy(required = false),
            // Default LicensePolicy is the strictest possible — storeContentAllowed = false.
            license = LicensePolicy(),
        )
    }
}

/**
 * Bundle of every adapter fan-out's result. Keeps [RoutingLexicalVerifier]'s
 * merge functions under the per-function parameter cap, and makes adding a
 * new sub-contract a field addition here plus a wire-up in `verify`.
 */
internal data class AdapterContributions(
    val lookups: List<LexicalEntryLookupResult>,
    val frequencies: List<FrequencyResult>,
    val cefrs: List<CefrResult>,
    val senses: List<SenseInventoryResult>,
    val familyResolutions: List<FamilyResolution>,
) {
    fun isEmpty(): Boolean =
        lookups.isEmpty() &&
            frequencies.isEmpty() &&
            cefrs.isEmpty() &&
            senses.isEmpty() &&
            familyResolutions.isEmpty()
}

internal data class FamilyResolution(
    val availability: VerifierAvailability,
    val unit: LexicalUnitInfo?,
    val confidence: Confidence,
    val family: FamilyResult?,
)

/**
 * Roll two availabilities together. Either Available wins outright; otherwise
 * prefer Degraded over Unavailable so a partial signal is still surfaced
 * upstream. Used by [RoutingLexicalVerifier.runFamilyResolution] to combine
 * `resolveUnit`'s availability with `family`'s.
 */
private fun bestAvailability(
    a: VerifierAvailability,
    b: VerifierAvailability?,
): VerifierAvailability {
    if (b == null) return a
    return when {
        a is VerifierAvailability.Available || b is VerifierAvailability.Available ->
            VerifierAvailability.Available
        a is VerifierAvailability.Degraded -> a
        b is VerifierAvailability.Degraded -> b
        else -> a
    }
}

private fun mergeFamily(
    base: FamilyContext?,
    resolutions: List<FamilyResolution>,
): FamilyContext? {
    // Only Available family resolutions feed the merge — a Degraded provider
    // may still carry a partial result (empty units, null lemma) that would
    // otherwise pollute the report's family with noise.
    // Rank by confidence (then a stable unit-id tie-break) so the resolved
    // unit and head lemma are deterministic: the `Set<LexicalFamilyProvider>`
    // Metro provides has no guaranteed iteration order, and two Available
    // providers can resolve the same term.
    val usable =
        resolutions
            .filter { it.availability is VerifierAvailability.Available }
            .sortedWith(
                compareByDescending<FamilyResolution> { it.confidence.ordinal }
                    .thenBy(nullsLast<String>()) { it.unit?.id?.value },
            )
    val providerUnits = usable.mapNotNull { it.unit }
    val providerSiblings = usable.flatMap { it.family?.units.orEmpty() }
    val providerLemma = usable.firstNotNullOfOrNull { it.family?.lemma }
    val truncated =
        (base?.truncated == true) || usable.any { it.family?.truncated == true }
    if (base == null && providerUnits.isEmpty() && providerSiblings.isEmpty()) return null
    val baseSiblings = base?.siblings.orEmpty()
    val merged = (baseSiblings + providerSiblings.map { it.toSummary() }).dedupByDisplayForm()
    val resolvedUnit = base?.resolvedUnit ?: providerUnits.firstOrNull()
    return FamilyContext(
        resolvedUnit = resolvedUnit,
        headLemma = base?.headLemma ?: providerLemma,
        siblings = merged,
        didYouMean = base?.didYouMean.orEmpty(),
        truncated = truncated,
    )
}

private fun mergeAvailability(
    base: VerifierAvailability,
    adapter: List<VerifierAvailability>,
): VerifierAvailability {
    // A single Available is enough — the orchestrator renders partial
    // evidence; all-Unavailable stays Unavailable so the UI knows nothing
    // was checked.
    val allUnavailable =
        base is VerifierAvailability.Unavailable &&
            adapter.all { it is VerifierAvailability.Unavailable }
    if (allUnavailable) return base
    val anyAvailable =
        adapter.any { it is VerifierAvailability.Available }
    return if (anyAvailable) {
        VerifierAvailability.Available
    } else {
        VerifierAvailability.Degraded("no source returned Available")
    }
}

private suspend fun runSenseInventory(
    provider: SenseInventoryProvider,
    query: LexicalVerificationQuery,
    logger: AppLogger,
): SenseInventoryResult {
    val timeoutMillis = query.policy.timeoutPerProviderMillis
    return runCatchingCancellable {
        val result = withTimeoutOrNull(timeoutMillis) { provider.senses(query) }
        if (result == null) {
            logger.warn {
                "Sense inventory adapter timed out: provider=${VerificationLogSummaries.providerName(provider)}, " +
                    "timeoutMs=$timeoutMillis"
            }
            degradedSense("provider timed out after ${timeoutMillis}ms")
        } else {
            result
        }
    }.getOrElse { throwable ->
        logger.warn(throwable) { "Sense inventory adapter raised; report carries no senses from it" }
        degradedSense("adapter raised: ${throwable.message ?: throwable::class.simpleName}")
    }
}

private fun mergeSenseMapping(
    base: SenseMapping,
    senses: List<SenseInventoryResult>,
): SenseMapping {
    // Drop Unavailable contributors so the merge cannot pull mapping evidence
    // from an adapter that explicitly opted out — the report-init invariant
    // forbids carrying evidence on an all-Unavailable report.
    val usable = senses.filter { it.availability !is VerifierAvailability.Unavailable }
    if (usable.isEmpty()) return base
    val mergedExtras: List<DictionarySenseSummary> =
        (base.extraDictionarySenses + usable.flatMap { it.mapping.extraDictionarySenses })
            // Two senses from the same source MUST keep distinct senseIds; otherwise
            // a polysemous lemma collapses to one summary.
            .distinctBy { Triple(it.ref.sourceId, it.ref.senseId, it.shortLabel) }
    return SenseMapping(
        extraDictionarySenses = mergedExtras,
        // Take the most confident contributor rather than the seed's default
        // `Low`; an empty provider mapping stays `Low` so it cannot inflate.
        confidence = maxOf(base.confidence, usable.maxOf { it.mapping.confidence }),
    )
}

private fun mergeNormalization(
    base: NormalizationOutcome,
    lookups: List<LexicalEntryLookupResult>,
): NormalizationOutcome {
    // Only Available/Degraded lookups feed normalization. An Unavailable
    // lookup that nonetheless filled `normalized.candidates` would violate the
    // [LexicalVerificationReport] init invariant (Unavailable ⇒ no evidence)
    // when it is the only contributor; gating defensively here keeps the
    // boundary safe against a misbehaving adapter without losing partial signal
    // from a Degraded one.
    val usableLookups = lookups.filter { it.availability !is VerifierAvailability.Unavailable }
    val newCandidates: List<NormalizationCandidate> =
        usableLookups.flatMap { it.normalized.candidates }
    if (newCandidates.isEmpty() && base.canonical != null) return base
    // Choose canonical by confidence, with the canonical string as a stable
    // tie-break. The `Set<LexicalEntryLookup>` Metro provides has no guaranteed
    // order, so `maxByOrNull` alone would let equally-confident lookups resolve
    // by Set iteration order — the secondary sort keeps the choice deterministic.
    val byConfidenceThenCanonical =
        compareByDescending<LexicalEntryLookupResult> { it.confidence.ordinal }
            .thenBy { it.normalized.canonical.orEmpty() }
    val canonical =
        base.canonical
            ?: usableLookups
                .filter { it.normalized.canonical != null }
                .sortedWith(byConfidenceThenCanonical)
                .firstOrNull()
                ?.normalized
                ?.canonical
    return NormalizationOutcome(
        canonical = canonical,
        candidates = base.candidates + newCandidates,
    )
}

private fun LexicalEntryLookupResult.toExistenceObservation(): Observation<LexicalExistence>? {
    val ref = sources.firstOrNull() ?: return null
    if (availability !is VerifierAvailability.Available) return null
    if (existence == LexicalExistence.Unknown) return null
    return Observation(existence, ref, confidence)
}

private fun LexicalEntryLookupResult.toPartsOfSpeechObservation(): Observation<List<PartOfSpeechHint>>? {
    val ref = sources.firstOrNull() ?: return null
    if (partsOfSpeech.isEmpty()) return null
    return Observation(partsOfSpeech, ref, confidence)
}

private fun LexicalEntryLookupResult.toEntryTypeObservation(): Observation<LexicalEntryTypeHint?>? {
    val entryType = entryType ?: return null
    val ref = sources.firstOrNull() ?: return null
    if (availability !is VerifierAvailability.Available) return null
    return Observation(entryType, ref, confidence)
}

private fun FrequencyResult.toFrequencyObservation(): Observation<FrequencyScore?>? {
    val score = score ?: return null
    val ref = sources.firstOrNull() ?: return null
    if (availability !is VerifierAvailability.Available) return null
    return Observation(score, ref, confidence)
}

private fun CefrResult.toCefrObservation(): Observation<CefrLevel?>? {
    val level = level ?: return null
    val ref = sources.firstOrNull() ?: return null
    if (availability !is VerifierAvailability.Available) return null
    return Observation(level, ref, confidence)
}

private fun LexicalEntryLookupResult.toPronunciationObservation(): Observation<PronunciationInfo?>? {
    val info = pronunciation ?: return null
    val ref = sources.firstOrNull() ?: return null
    if (availability !is VerifierAvailability.Available) return null
    return Observation(info, ref, confidence)
}

private fun <T> EvidenceSet<T>.append(extra: List<Observation<T>>): EvidenceSet<T> =
    if (extra.isEmpty()) this else EvidenceSet(observations + extra)

private fun LexicalUnitInfo.toSummary(): LexicalUnitSummary =
    LexicalUnitSummary(
        id = id,
        displayForm = displayForm,
        entryType = entryType,
        sources = sources,
    )

private fun List<LexicalUnitSummary>.dedupByDisplayForm(): List<LexicalUnitSummary> {
    val seen = mutableSetOf<String>()
    return filter { seen.add(it.displayForm.lowercase()) }
}
