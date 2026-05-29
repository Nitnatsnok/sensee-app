package app.sensee.verification.integration

import app.sensee.verification.core.CefrResult
import app.sensee.verification.core.Confidence
import app.sensee.verification.core.ExampleCheckResult
import app.sensee.verification.core.FrequencyResult
import app.sensee.verification.core.LexicalEntryLookupResult
import app.sensee.verification.core.LexicalExistence
import app.sensee.verification.core.NormalizationOutcome
import app.sensee.verification.core.SenseInventoryResult
import app.sensee.verification.core.SenseMapping
import app.sensee.verification.core.VerifierAvailability

/**
 * Per-sub-contract `Degraded` factory results used by [RoutingLexicalVerifier]
 * when an adapter raises an exception, returns nothing, or times out. Each
 * carries the failure reason in `availability.reason` so the merged report's
 * provenance surface is debuggable downstream.
 *
 * Kept in its own file so the routing class can host enough run* wrappers
 * without tripping detekt's per-file function cap.
 */

internal fun degradedLookup(reason: String): LexicalEntryLookupResult =
    LexicalEntryLookupResult(
        availability = VerifierAvailability.Degraded(reason),
        existence = LexicalExistence.Unknown,
        normalized = NormalizationOutcome.EMPTY,
        partsOfSpeech = emptyList(),
        confidence = Confidence.Low,
        sources = emptyList(),
    )

internal fun degradedFrequency(reason: String): FrequencyResult =
    FrequencyResult(
        availability = VerifierAvailability.Degraded(reason),
        score = null,
        confidence = Confidence.Low,
        sources = emptyList(),
    )

internal fun degradedCefr(reason: String): CefrResult =
    CefrResult(
        availability = VerifierAvailability.Degraded(reason),
        level = null,
        confidence = Confidence.Low,
        sources = emptyList(),
    )

internal fun degradedExample(reason: String): ExampleCheckResult =
    ExampleCheckResult(
        availability = VerifierAvailability.Degraded(reason),
        issues = emptyList(),
        rewrite = null,
        sources = emptyList(),
    )

internal fun degradedFamily(reason: String): FamilyResolution =
    FamilyResolution(
        availability = VerifierAvailability.Degraded(reason),
        unit = null,
        confidence = Confidence.Low,
        family = null,
    )

internal fun degradedSense(reason: String): SenseInventoryResult =
    SenseInventoryResult(
        availability = VerifierAvailability.Degraded(reason),
        mapping = SenseMapping.EMPTY,
        sources = emptyList(),
    )
