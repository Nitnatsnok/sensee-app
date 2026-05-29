package app.sensee.feature.vocabularyEditor.domain

import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentExample
import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.core.EnrichmentSuggestion
import app.sensee.ai.core.GrammarTagHint
import app.sensee.ai.core.IrregularFormsHint
import app.sensee.ai.core.PrepositionGovernmentHint
import app.sensee.ai.core.UnitComponentHint
import app.sensee.ai.core.UsageLabelHint
import app.sensee.ai.core.WordFamilyHint
import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.ComponentSalience
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.PrepositionGovernment
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.grammar.domain.TaxonomyInvariants
import app.sensee.grammar.domain.UsageAxis
import app.sensee.grammar.domain.UsageLabel
import app.sensee.grammar.domain.UsageValue
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.WordFamilyMember
import kotlin.test.Test
import kotlin.test.assertEquals

class SenseCandidateMapperTest {
    private val testInvariants =
        TaxonomyInvariants(
            knownUnitTypeIds = setOf("phrasal_verb", "noun", "verb", "irregular_verb"),
            knownComplementIds = setOf("prepositional_phrase"),
            allowedValuesByAxis =
                mapOf(
                    "register" to setOf("formal", "informal", "slang", "neutral"),
                ),
            allowedFormsByCategory =
                mapOf(
                    "verb_irregular" to setOf("infinitive", "past_tense", "past_participle"),
                    "number" to setOf("singular", "plural"),
                ),
        )

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
                                listOf(
                                    EnrichmentExample(sentence = "She [[comes across]] as shy."),
                                    EnrichmentExample(sentence = "He came across well."),
                                ),
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

        val candidate =
            result.toSenseCandidates(fallbackTerm = "come across", invariants = testInvariants).single()

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
    fun `synonyms collocations derivatives and component salience reach the candidate`() {
        val suggestion =
            EnrichmentSuggestion(
                translation = "наткнуться",
                surfaceForm = "come across",
                unitType = "phrasal_verb",
                baseLemma = "come",
                headLemma = "come",
                components =
                    listOf(
                        UnitComponentHint("come", "head", "primary"),
                        UnitComponentHint("across", "particle", "secondary"),
                    ),
                synonyms = listOf("encounter", "stumble upon"),
                antonyms = listOf("avoid"),
                collocations = listOf("come across a problem"),
                wordFamily = listOf(WordFamilyHint("comer", "noun")),
            )

        val candidate = suggestion.toSenseCandidate(fallbackTerm = "come across", invariants = testInvariants)

        assertEquals(listOf("encounter", "stumble upon"), candidate.synonyms)
        assertEquals(listOf("avoid"), candidate.antonyms)
        assertEquals(listOf("come across a problem"), candidate.collocations)
        assertEquals(listOf(WordFamilyMember("comer", GrammarUnitType.Noun)), candidate.wordFamily)
        assertEquals(
            listOf(ComponentSalience.Primary, ComponentSalience.Secondary),
            candidate.components.map { it.salience },
        )
    }

    @Test
    fun `irregular forms upgrade a plain verb candidate to irregular verb`() {
        val suggestion =
            EnrichmentSuggestion(
                translation = "приходить",
                surfaceForm = "come",
                unitType = "verb",
                irregularForms = IrregularFormsHint("come", "came", "come"),
            )

        val candidate = suggestion.toSenseCandidate(fallbackTerm = "come", invariants = testInvariants)

        assertEquals(GrammarUnitType.IrregularVerb, candidate.unitType)
        assertEquals(IrregularForms("come", "came", "come"), candidate.irregularForms)
    }

    @Test
    fun `irregular forms infer irregular verb when unit type is missing`() {
        val suggestion =
            EnrichmentSuggestion(
                translation = "приходить",
                surfaceForm = "come",
                irregularForms = IrregularFormsHint("come", "came", "come"),
            )

        val candidate = suggestion.toSenseCandidate(fallbackTerm = "come", invariants = testInvariants)

        assertEquals(GrammarUnitType.IrregularVerb, candidate.unitType)
    }

    @Test
    fun `a blank surface form falls back to the captured term`() {
        val result =
            EnrichmentResult(
                availability = EnrichmentAvailability.Available,
                suggestions = listOf(EnrichmentSuggestion(translation = "бежать")),
            )

        val candidate = result.toSenseCandidates(fallbackTerm = "run").single()

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

        val candidate = suggestion.toSenseCandidate(fallbackTerm = "cats", invariants = testInvariants)

        assertEquals(
            listOf(GrammarTag(GrammarCategory.Number, GrammarForm.Plural)),
            candidate.grammarTags,
        )
    }

    @Test
    fun `strict mode validates normalized unit and complement ids by canonical id`() {
        val suggestion =
            EnrichmentSuggestion(
                translation = "произвести впечатление",
                unitType = "PhrasalVerb",
                complementation = listOf("prepositional-phrase"),
            )

        val candidate = suggestion.toSenseCandidate(fallbackTerm = "come across", invariants = testInvariants)

        assertEquals(GrammarUnitType.PhrasalVerb, candidate.unitType)
        assertEquals(listOf(ComplementType.PrepositionalPhrase), candidate.complementation)
    }

    @Test
    fun `the same sense maps to the same candidate id across re-enrichment`() {
        val suggestion =
            EnrichmentSuggestion(
                translation = "производить впечатление",
                surfaceForm = "come across [as]",
                unitType = "phrasal_verb",
            )

        val first = suggestion.toSenseCandidate(fallbackTerm = "come across")
        val second = suggestion.toSenseCandidate(fallbackTerm = "come across")

        assertEquals(first.id, second.id)
    }

    @Test
    fun `distinct senses of one term get distinct candidate ids`() {
        val result =
            EnrichmentResult(
                availability = EnrichmentAvailability.Available,
                suggestions =
                    listOf(
                        EnrichmentSuggestion(translation = "наткнуться", surfaceForm = "come across"),
                        EnrichmentSuggestion(
                            translation = "произвести впечатление",
                            surfaceForm = "come across [as]",
                        ),
                    ),
            )

        val ids = result.toSenseCandidates(fallbackTerm = "come across").map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `same translation senses with different details get distinct candidate ids`() {
        val result =
            EnrichmentResult(
                availability = EnrichmentAvailability.Available,
                suggestions =
                    listOf(
                        EnrichmentSuggestion(
                            translation = "идти",
                            surfaceForm = "go",
                            unitType = "phrasal_verb",
                            explanation = "move from one place to another",
                        ),
                        EnrichmentSuggestion(
                            translation = "идти",
                            surfaceForm = "go",
                            unitType = "phrasal_verb",
                            explanation = "function or proceed",
                        ),
                    ),
            )

        val ids = result.toSenseCandidates(fallbackTerm = "go").map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `duplicate assistant suggestions are disambiguated for selection keys`() {
        val result =
            EnrichmentResult(
                availability = EnrichmentAvailability.Available,
                suggestions =
                    listOf(
                        EnrichmentSuggestion(translation = "идти", surfaceForm = "go"),
                        EnrichmentSuggestion(translation = "идти", surfaceForm = "go"),
                    ),
            )

        val ids = result.toSenseCandidates(fallbackTerm = "go").map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `confirmed sense key ignores verifier lemma metadata`() {
        val withoutMetadata =
            Sense(
                translation = "наткнуться",
                surfaceForm = SurfaceForm.parse("come across"),
                unitType = GrammarUnitType.PhrasalVerb,
            )
        val withMetadata =
            withoutMetadata.copy(
                baseLemma = "come across",
                headLemma = "come",
            )

        assertEquals(deriveSenseContentKey(withoutMetadata), deriveSenseContentKey(withMetadata))
    }

    @Test
    fun `confirmed sense key ignores surface-form case so a re-capture does not duplicate`() {
        val lower =
            Sense(
                translation = "наткнуться",
                surfaceForm = SurfaceForm.parse("come across"),
                unitType = GrammarUnitType.PhrasalVerb,
                headLemma = "come",
            )
        val capitalized = lower.copy(surfaceForm = SurfaceForm.parse("Come across"))

        assertEquals(deriveSenseContentKey(lower), deriveSenseContentKey(capitalized))
    }
}
