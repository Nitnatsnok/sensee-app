package app.sensee.feature.vocabularyEditor.domain.usecase

import app.sensee.verification.core.contract.AttributionPolicy
import app.sensee.verification.core.contract.Confidence
import app.sensee.verification.core.contract.EvidenceSet
import app.sensee.verification.core.contract.LexicalEntryTypeHint
import app.sensee.verification.core.contract.LexicalExistence
import app.sensee.verification.core.contract.LexicalSource
import app.sensee.verification.core.contract.LexicalSourceRef
import app.sensee.verification.core.contract.LexicalVerificationReport
import app.sensee.verification.core.contract.LicensePolicy
import app.sensee.verification.core.contract.Observation
import app.sensee.verification.core.contract.PartOfSpeechHint
import app.sensee.verification.core.contract.VerifierAvailability
import app.sensee.verification.core.hierarchy.FamilyContext
import app.sensee.verification.core.hierarchy.LemmaId
import app.sensee.verification.core.hierarchy.LexicalLemma
import app.sensee.verification.core.hierarchy.LexicalUnitId
import app.sensee.verification.core.hierarchy.LexicalUnitInfo
import app.sensee.verification.core.hierarchy.LexicalUnitSummary
import app.sensee.verification.core.hierarchy.UnitComponentFact

internal object VerifierReports {
    fun simplePhrasalVerb(
        usableAsLlmContext: Boolean,
        includeFamilySources: Boolean = true,
    ): LexicalVerificationReport {
        val ref =
            LexicalSourceRef(
                sourceId = "test-fixture",
                entryId = "come_across",
                fetchedAtEpochMillis = 0L,
            )
        val familySources = if (includeFamilySources) listOf(ref) else emptyList()
        val source =
            LexicalSource.Adapter(
                id = "test-fixture",
                displayName = "Test fixture",
                attribution = AttributionPolicy(required = false),
                license = LicensePolicy(usableAsLlmContext = usableAsLlmContext),
            )
        return LexicalVerificationReport(
            availability = VerifierAvailability.Available,
            existence =
                EvidenceSet(listOf(Observation(LexicalExistence.Confirmed, ref, Confidence.High))),
            entryType =
                EvidenceSet(
                    listOf<Observation<LexicalEntryTypeHint?>>(
                        Observation(LexicalEntryTypeHint("phrasal_verb"), ref, Confidence.High),
                    ),
                ),
            partsOfSpeech =
                EvidenceSet(
                    listOf(
                        Observation(listOf(PartOfSpeechHint("verb")), ref, Confidence.High),
                    ),
                ),
            family =
                FamilyContext(
                    resolvedUnit =
                        LexicalUnitInfo(
                            id =
                                LexicalUnitId.of(
                                    "en",
                                    "come across",
                                    LexicalEntryTypeHint("phrasal_verb"),
                                ),
                            displayForm = "come across",
                            entryType = LexicalEntryTypeHint("phrasal_verb"),
                            headLemma = LemmaId.of("en", "come"),
                            components =
                                listOf(
                                    UnitComponentFact("come", "head"),
                                    UnitComponentFact("across", "particle"),
                                ),
                            sources = familySources,
                        ),
                    headLemma =
                        LexicalLemma(
                            id = LemmaId.of("en", "come"),
                            canonical = "come",
                            language = "en",
                            pos = PartOfSpeechHint("verb"),
                            sources = familySources,
                        ),
                    siblings =
                        listOf(
                            LexicalUnitSummary(
                                id =
                                    LexicalUnitId.of(
                                        "en",
                                        "come across",
                                        LexicalEntryTypeHint("phrasal_verb"),
                                    ),
                                displayForm = "come across",
                                entryType = LexicalEntryTypeHint("phrasal_verb"),
                                sources = familySources,
                            ),
                        ),
                ),
            sources = listOf(source),
        )
    }
}
