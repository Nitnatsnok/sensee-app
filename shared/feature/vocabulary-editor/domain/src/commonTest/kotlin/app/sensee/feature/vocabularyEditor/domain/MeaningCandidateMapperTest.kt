package app.sensee.feature.vocabularyEditor.domain

import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.core.EnrichmentSuggestion
import app.sensee.ai.core.GrammarTagHint
import app.sensee.ai.core.IrregularFormsHint
import app.sensee.ai.core.PrepositionGovernmentHint
import app.sensee.ai.core.UsageLabelHint
import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.PrepositionGovernment
import app.sensee.grammar.domain.UsageAxis
import app.sensee.grammar.domain.UsageLabel
import app.sensee.grammar.domain.UsageValue
import kotlin.test.Test
import kotlin.test.assertEquals

class MeaningCandidateMapperTest {
    @Test
    fun `a suggestion maps onto a structured candidate at the feature boundary`() {
        val result =
            EnrichmentResult(
                availability = EnrichmentAvailability.Available,
                suggestions =
                    listOf(
                        EnrichmentSuggestion(
                            translation = "произвести впечатление",
                            surfaceForm = "come across [as]",
                            unitType = "phrasal_verb",
                            baseLemma = "come",
                            explanation = "to give a particular impression",
                            examples =
                                listOf("She [[comes across]] as shy.", "He came across well."),
                            governedPrepositions = listOf(PrepositionGovernmentHint(listOf("as"))),
                            complementation = listOf("prepositional_phrase", "bogus"),
                            usageLabels =
                                listOf(
                                    UsageLabelHint("register", "informal"),
                                    UsageLabelHint("register", "archaic"),
                                ),
                            usageNote = "of impressions",
                            grammarTags = listOf(GrammarTagHint("verb_irregular", "infinitive")),
                            irregularForms = IrregularFormsHint("come", "came", "come"),
                        ),
                    ),
            )

        val candidate = result.toMeaningCandidates(fallbackTerm = "come across").single()

        assertEquals("произвести впечатление", candidate.translation)
        assertEquals("come across [as]", candidate.surfaceForm?.display())
        assertEquals(GrammarUnitType.PhrasalVerb, candidate.unitType)
        assertEquals("come", candidate.baseLemma)
        assertEquals(2, candidate.contextualApplications.size)
        val firstSentence = candidate.contextualApplications.first().sentence
        assertEquals("comes across", firstSentence.target)
        assertEquals("She comes across as shy.", firstSentence.plainText())
        assertEquals(listOf(PrepositionGovernment(listOf("as"))), candidate.governedPrepositions)
        // Unknown complement id ("bogus") is dropped at the boundary.
        assertEquals(listOf(ComplementType.PrepositionalPhrase), candidate.complementation)
        // Off-axis label (register/archaic) is dropped; valid one resolves.
        assertEquals(
            listOf(UsageLabel(UsageAxis.Register, UsageValue.Informal)),
            candidate.usageLabels,
        )
        assertEquals("of impressions", candidate.usageNote)
        assertEquals(
            GrammarTag(GrammarCategory.VerbIrregular, GrammarForm.Infinitive),
            candidate.grammarTags.single(),
        )
        assertEquals(IrregularForms("come", "came", "come"), candidate.irregularForms)
    }

    @Test
    fun `a blank surface form falls back to the captured term`() {
        val result =
            EnrichmentResult(
                availability = EnrichmentAvailability.Available,
                suggestions = listOf(EnrichmentSuggestion(translation = "бежать")),
            )

        val candidate = result.toMeaningCandidates(fallbackTerm = "run").single()

        assertEquals("run", candidate.surfaceForm?.display())
    }

    @Test
    fun `a grammar pair that breaks the category invariant is dropped not constructed`() {
        val suggestion =
            EnrichmentSuggestion(
                translation = "кошка",
                grammarTags =
                    listOf(
                        GrammarTagHint("number", "plural"),
                        GrammarTagHint("number", "past_tense"),
                        GrammarTagHint("unknown", "nope"),
                    ),
            )

        val candidate = suggestion.toMeaningCandidate(fallbackTerm = "cats")

        assertEquals(
            listOf(GrammarTag(GrammarCategory.Number, GrammarForm.Plural)),
            candidate.grammarTags,
        )
    }
}
