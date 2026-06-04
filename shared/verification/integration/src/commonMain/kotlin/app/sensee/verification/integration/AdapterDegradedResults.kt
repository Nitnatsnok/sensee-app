package app.sensee.verification.integration

import app.sensee.verification.core.contract.CefrResult
import app.sensee.verification.core.contract.Confidence
import app.sensee.verification.core.contract.FrequencyResult
import app.sensee.verification.core.contract.LexicalEntryLookupResult
import app.sensee.verification.core.contract.LexicalExistence
import app.sensee.verification.core.contract.NormalizationOutcome
import app.sensee.verification.core.contract.SenseInventoryResult
import app.sensee.verification.core.contract.VerifierAvailability
import app.sensee.verification.core.grounding.SenseMapping

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
