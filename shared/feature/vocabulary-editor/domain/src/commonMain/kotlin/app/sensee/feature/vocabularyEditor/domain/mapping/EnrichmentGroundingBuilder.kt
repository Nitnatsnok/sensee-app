package app.sensee.feature.vocabularyEditor.domain.mapping

import app.sensee.ai.core.grounding.EnrichmentGrounding
import app.sensee.ai.core.grounding.FamilyFact
import app.sensee.ai.core.grounding.FamilySiblingFact
import app.sensee.ai.core.grounding.FrequencyFact
import app.sensee.ai.core.grounding.GroundingConfidence
import app.sensee.ai.core.grounding.GroundingSourceAttribution
import app.sensee.ai.core.grounding.NormalizationFact
import app.sensee.ai.core.grounding.PronunciationFact
import app.sensee.ai.core.grounding.SenseSummary
import app.sensee.ai.core.grounding.UnitComponentFact
import app.sensee.ai.core.grounding.UnitFact
import app.sensee.verification.core.contract.Confidence
import app.sensee.verification.core.contract.EvidenceSet
import app.sensee.verification.core.contract.LexicalVerificationReport
import app.sensee.verification.core.contract.VerifierAvailability
import app.sensee.verification.core.hierarchy.FamilyContext
import app.sensee.verification.core.hierarchy.LemmaId
import app.sensee.verification.core.hierarchy.LexicalLemma
import app.sensee.verification.core.hierarchy.LexicalUnitInfo
import app.sensee.verification.core.hierarchy.LexicalUnitSummary

/**
 * Maps a verification report into [EnrichmentGrounding] for the AI seam. Two
 * load-bearing filters live here, not in the seam: (1) only Available /
 * Degraded reports produce grounding — Unavailable yields `null`; (2) each
 * observation is dropped when its source's license does NOT permit
 * `usableAsLlmContext`. The fence is intentional: an adapter that disallows
 * third-party-LLM context cannot leak its content into a provider prompt.
 *
 * The verifier side keeps the term "evidence" (`EvidenceSet`); the AI side this
 * crosses into is the grounding channel.
 */
public fun LexicalVerificationReport.toEnrichmentGrounding(): EnrichmentGrounding? {
    if (availability is VerifierAvailability.Unavailable) return null
    val allowed = sources.filter { it.license.usableAsLlmContext }.map { it.id }.toSet()
    if (allowed.isEmpty()) return null
    val grounding =
        EnrichmentGrounding(
            normalized = buildNormalization(allowed),
            lemma =
                family?.headLemma?.takeIf { it.allowedBy(allowed) }?.canonical
                    ?: family
                        ?.resolvedUnit
                        ?.takeIf { it.allowedBy(allowed) }
                        ?.headLemma
                        ?.plainValue(),
            entryType = entryType.allowedConsensus(allowed)?.id,
            unit = buildUnitFact(allowed),
            knownPartsOfSpeech = partsOfSpeech.allowedConsensus(allowed).orEmpty().map { it.id },
            knownSenseSummaries = buildKnownSenses(allowed),
            cefr = cefr.allowedConsensus(allowed)?.name,
            frequency =
                frequency.allowedConsensus(allowed)?.let { score ->
                    FrequencyFact(zipf = score.zipf, band = score.band?.name)
                },
            pronunciations = buildPronunciations(allowed),
            family = buildFamilyFact(allowed),
            sources =
                sources
                    .filter { it.id in allowed }
                    .map { GroundingSourceAttribution(sourceId = it.id, displayName = it.displayName) },
            confidence = aggregateConfidence(allowed),
        )
    return grounding.takeIf { it.isInformative() }
}

private fun LexicalVerificationReport.buildNormalization(allowed: Set<String>): NormalizationFact? {
    if (normalized.canonical == null) return null
    // Emit the allowed source's own value, not the report-level consensus,
    // so the LLM only ever sees text from a usableAsLlmContext source.
    val backing = normalized.candidates.firstOrNull { it.source.sourceId in allowed } ?: return null
    return NormalizationFact(canonical = backing.text, kind = backing.kind.name)
}

private fun LexicalVerificationReport.buildUnitFact(allowed: Set<String>): UnitFact? {
    val unit = family?.resolvedUnit ?: return null
    if (!unit.allowedBy(allowed)) return null
    return UnitFact(
        displayForm = unit.displayForm,
        entryType = unit.entryType.id,
        headLemma = unit.headLemma?.plainValue(),
        components = unit.components.map { UnitComponentFact(it.text, it.role) },
    )
}

private fun LexicalVerificationReport.buildKnownSenses(allowed: Set<String>): List<SenseSummary> =
    senseMapping.extraDictionarySenses
        .filter { it.ref.sourceId in allowed }
        .map { extra ->
            SenseSummary(
                pos = extra.pos?.id,
                shortLabel = extra.shortLabel,
                cefr = extra.cefr?.name,
            )
        }

private fun LexicalVerificationReport.buildPronunciations(allowed: Set<String>): List<PronunciationFact> =
    pronunciation
        .observations
        .filter { it.source.sourceId in allowed }
        .mapNotNull { it.value }
        .flatMap { info ->
            info.variants.map { variant ->
                PronunciationFact(accent = variant.accent, ipa = variant.ipa)
            }
        }

private fun LexicalVerificationReport.buildFamilyFact(allowed: Set<String>): FamilyFact? {
    val context: FamilyContext = family ?: return null
    val head = context.headLemma ?: return null
    if (!head.allowedBy(allowed)) return null
    val siblings =
        context.siblings.filter { it.allowedBy(allowed) }.map { sibling ->
            FamilySiblingFact(displayForm = sibling.displayForm, entryType = sibling.entryType.id)
        }
    if (siblings.isEmpty()) return null
    return FamilyFact(head = head.canonical, siblings = siblings, truncated = context.truncated)
}

private fun LexicalVerificationReport.aggregateConfidence(allowed: Set<String>): GroundingConfidence {
    val confidences =
        sequenceOf(
            existence.observations,
            entryType.observations,
            cefr.observations,
            frequency.observations,
        ).flatten()
            .filter { it.source.sourceId in allowed }
            .map { it.confidence }
            .toList()
    if (confidences.isEmpty()) return GroundingConfidence.Low
    val highCount = confidences.count { it == Confidence.High }
    val mediumCount = confidences.count { it == Confidence.Medium }
    return when {
        highCount * 2 >= confidences.size -> GroundingConfidence.High
        (highCount + mediumCount) * 2 >= confidences.size -> GroundingConfidence.Medium
        else -> GroundingConfidence.Low
    }
}

private fun EnrichmentGrounding.isInformative(): Boolean =
    normalized != null ||
        lemma != null ||
        entryType != null ||
        unit != null ||
        knownPartsOfSpeech.isNotEmpty() ||
        knownSenseSummaries.isNotEmpty() ||
        cefr != null ||
        frequency != null ||
        pronunciations.isNotEmpty() ||
        family != null

private fun <T> EvidenceSet<T>.allowedConsensus(allowed: Set<String>): T? {
    val filtered = observations.filter { it.source.sourceId in allowed }
    if (filtered.isEmpty()) return null
    val values = filtered.map { it.value }.toSet()
    return if (values.size == 1) values.single() else null
}

private fun LexicalUnitInfo.allowedBy(allowed: Set<String>): Boolean = sources.any { it.sourceId in allowed }

private fun LexicalLemma.allowedBy(allowed: Set<String>): Boolean = sources.any { it.sourceId in allowed }

private fun LexicalUnitSummary.allowedBy(allowed: Set<String>): Boolean = sources.any { it.sourceId in allowed }

private fun LemmaId.plainValue(): String = value.substringAfter(':')
