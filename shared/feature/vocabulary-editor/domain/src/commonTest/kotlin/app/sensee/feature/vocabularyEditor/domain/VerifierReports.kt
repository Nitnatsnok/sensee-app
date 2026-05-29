package app.sensee.feature.vocabularyEditor.domain

import app.sensee.verification.core.AttributionPolicy
import app.sensee.verification.core.Confidence
import app.sensee.verification.core.EvidenceSet
import app.sensee.verification.core.LexicalEntryTypeHint
import app.sensee.verification.core.LexicalExistence
import app.sensee.verification.core.LexicalSource
import app.sensee.verification.core.LexicalSourceRef
import app.sensee.verification.core.LexicalVerificationReport
import app.sensee.verification.core.LicensePolicy
import app.sensee.verification.core.Observation
import app.sensee.verification.core.PartOfSpeechHint
import app.sensee.verification.core.VerifierAvailability

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
                app.sensee.verification.core.FamilyContext(
                    resolvedUnit =
                        app.sensee.verification.core.LexicalUnitInfo(
                            id =
                                app.sensee.verification.core.LexicalUnitId.of(
                                    "en",
                                    "come across",
                                    LexicalEntryTypeHint("phrasal_verb"),
                                ),
                            displayForm = "come across",
                            entryType = LexicalEntryTypeHint("phrasal_verb"),
                            headLemma =
                                app.sensee.verification.core.LemmaId
                                    .of("en", "come"),
                            components =
                                listOf(
                                    app.sensee.verification.core
                                        .UnitComponentFact("come", "head"),
                                    app.sensee.verification.core
                                        .UnitComponentFact("across", "particle"),
                                ),
                            sources = familySources,
                        ),
                    headLemma =
                        app.sensee.verification.core.LexicalLemma(
                            id =
                                app.sensee.verification.core.LemmaId
                                    .of("en", "come"),
                            canonical = "come",
                            language = "en",
                            pos = PartOfSpeechHint("verb"),
                            sources = familySources,
                        ),
                    siblings =
                        listOf(
                            app.sensee.verification.core.LexicalUnitSummary(
                                id =
                                    app.sensee.verification.core.LexicalUnitId.of(
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
