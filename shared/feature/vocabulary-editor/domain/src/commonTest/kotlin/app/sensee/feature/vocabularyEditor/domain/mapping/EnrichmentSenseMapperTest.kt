package app.sensee.feature.vocabularyEditor.domain.mapping

import app.sensee.ai.core.model.CefrEnrichmentExtension
import app.sensee.ai.core.model.EnrichmentExample
import app.sensee.ai.core.model.EnrichmentSuggestion
import app.sensee.ai.core.model.GrammarTagHint
import app.sensee.ai.core.model.IrregularFormsHint
import app.sensee.ai.core.model.UnitComponentHint
import app.sensee.grammar.domain.ComponentRole
import app.sensee.grammar.domain.ComponentSalience
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.TaxonomyInvariants
import app.sensee.lexicon.domain.CefrLevel
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnrichmentSenseMapperTest {
    private val testInvariants =
        TaxonomyInvariants(
            knownUnitTypeIds = setOf("phrasal_verb", "verb", "irregular_verb"),
            knownComplementIds = emptySet(),
            allowedValuesByAxis = emptyMap(),
            allowedFormsByCategory =
                mapOf(
                    "verb_irregular" to setOf("infinitive", "past_tense", "past_participle"),
                ),
        )

    @Test
    fun `an enrichment suggestion maps onto a central sense`() {
        val sense =
            EnrichmentSuggestion(
                translation = "произвести впечатление",
                surfaceForm = "come across [as]",
                unitType = "phrasal_verb",
                baseLemma = "come across",
                headLemma = "come",
                components =
                    listOf(
                        UnitComponentHint("come", "head", "primary"),
                        UnitComponentHint("across", "particle", "secondary"),
                    ),
                examples = listOf(EnrichmentExample(sentence = "She [[comes across]] as shy.")),
                grammarTags = listOf(GrammarTagHint("verb_irregular", "infinitive")),
            ).toSense(fallbackTerm = "come across", invariants = testInvariants)

        assertEquals("произвести впечатление", sense.translation)
        assertEquals("come across [as]", sense.surfaceForm?.display())
        assertEquals(GrammarUnitType.PhrasalVerb, sense.unitType)
        assertEquals("come", sense.headLemma)
        assertEquals(
            listOf(ComponentRole.Head, ComponentRole.Particle),
            sense.components.map { it.role },
        )
        assertEquals(
            listOf(ComponentSalience.Primary, ComponentSalience.Secondary),
            sense.components.map { it.salience },
        )
        assertEquals(
            "comes across",
            sense.contextualApplications
                .single()
                .sentence
                .target,
        )
        assertEquals(
            GrammarTag(GrammarCategory.VerbIrregular, GrammarForm.Infinitive),
            sense.grammarTags.single(),
        )
    }

    @Test
    fun `irregular forms upgrade a plain verb to irregular verb`() {
        val sense =
            EnrichmentSuggestion(
                translation = "приходить",
                surfaceForm = "come",
                unitType = "verb",
                irregularForms = IrregularFormsHint("come", "came", "come"),
            ).toSense(fallbackTerm = "come", invariants = testInvariants)

        assertEquals(GrammarUnitType.IrregularVerb, sense.unitType)
        assertEquals(IrregularForms("come", "came", "come"), sense.irregularForms)
    }

    @Test
    fun `a cefr extension maps onto the sense cefr level`() {
        val sense =
            EnrichmentSuggestion(
                translation = "приходить",
                surfaceForm = "come",
                unitType = "verb",
                extensions = mapOf(CefrEnrichmentExtension.KEY to JsonPrimitive("B2")),
            ).toSense(fallbackTerm = "come", invariants = testInvariants)

        assertEquals(CefrLevel.B2, sense.cefr)
    }

    @Test
    fun `an absent cefr extension leaves the sense cefr null`() {
        val sense =
            EnrichmentSuggestion(translation = "приходить", surfaceForm = "come", unitType = "verb")
                .toSense(fallbackTerm = "come", invariants = testInvariants)

        assertNull(sense.cefr)
    }
}
