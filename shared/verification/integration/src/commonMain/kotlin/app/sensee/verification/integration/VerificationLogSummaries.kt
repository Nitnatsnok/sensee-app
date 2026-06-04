package app.sensee.verification.integration

import app.sensee.verification.core.contract.LexicalVerificationQuery
import app.sensee.verification.core.contract.LexicalVerificationReport
import app.sensee.verification.core.contract.VerifierAvailability

internal object VerificationLogSummaries {
    fun query(query: LexicalVerificationQuery): String {
        val nativeLanguage = query.nativeLanguageTag ?: "none"
        val entryType = query.expectedEntryType?.id ?: "none"
        val partOfSpeech = query.expectedPartOfSpeech?.id ?: "none"
        val maxCacheAge = query.policy.maxCacheAgeMillis ?: "default"
        return "query(studyLanguage=${query.studyLanguageTag}, nativeLanguage=$nativeLanguage, " +
            "expectedEntryType=$entryType, expectedPartOfSpeech=$partOfSpeech, " +
            "allowNetwork=${query.policy.allowNetwork}, " +
            "includeFamily=${query.policy.includeFamily}, familySiblingCap=${query.policy.familySiblingCap}, " +
            "timeoutMs=${query.policy.timeoutPerProviderMillis}, maxCacheAgeMs=$maxCacheAge)"
    }

    fun report(report: LexicalVerificationReport): String =
        "report(availability=${availabilityLabel(report.availability)}, " +
            "sourceIds=${report.sources.map { it.id }.sorted()}, " +
            "existence=${report.existence.observations.size}, " +
            "partsOfSpeech=${report.partsOfSpeech.observations.size}, " +
            "frequency=${report.frequency.observations.size}, cefr=${report.cefr.observations.size}, " +
            "pronunciation=${report.pronunciation.observations.size}, " +
            "senseExtras=${report.senseMapping.extraDictionarySenses.size}, " +
            "family=${report.family != null}, " +
            "findings=${report.findings.size})"

    fun contributions(contributions: AdapterContributions): String =
        "contributions(lookups=${contributions.lookups.map { it.availability }.availabilityCounts()}, " +
            "frequencies=${contributions.frequencies.map { it.availability }.availabilityCounts()}, " +
            "cefr=${contributions.cefrs.map { it.availability }.availabilityCounts()}, " +
            "senses=${contributions.senses.map { it.availability }.availabilityCounts()}, " +
            "families=${contributions.familyResolutions.map { it.availability }.availabilityCounts()})"

    fun l2SkipReason(report: LexicalVerificationReport): String =
        when {
            report.availability !is VerifierAvailability.Available ->
                "availability-${availabilityLabel(report.availability)}"
            report.sources.isEmpty() -> "source-metadata-empty"
            report.sources.any { !it.license.storeContentAllowed } -> "strict-license-source"
            else -> "unknown"
        }

    fun availabilityLabel(availability: VerifierAvailability): String =
        when (availability) {
            VerifierAvailability.Available -> "available"
            is VerifierAvailability.Degraded -> "degraded"
            is VerifierAvailability.Unavailable -> "unavailable"
        }

    fun providerName(provider: Any): String = provider::class.simpleName ?: "anonymous"

    private fun List<VerifierAvailability>.availabilityCounts(): String =
        "available=${count { it is VerifierAvailability.Available }}," +
            "degraded=${count { it is VerifierAvailability.Degraded }}," +
            "unavailable=${count { it is VerifierAvailability.Unavailable }}"
}
