package app.sensee.ai.core

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies the provider-boundary enrichment schema on representative lexical
 * cases: distinct senses, fixed surface forms, governed prepositions, and
 * irregular verb principal parts.
 */
class EnrichmentSchemaContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun map(raw: String) = EnrichmentResponseMapper.map(json.decodeFromString<EnrichmentResponseV1>(raw))

    @Test
    fun `come - irregular verb keeps stored forms and splits the come-to-verb pattern`() {
        val result =
            map(
                """
                {"version": 1,"items":[
                  {"translation":"приходить","surface_form":"come","unit_type":"irregular_verb",
                   "base_lemma":"come","explanation":"двигаться по направлению к говорящему",
                   "examples":[{"sentence":"[[Come]] here."}],
                   "grammar_tags":[{"category":"verb_irregular","form":"infinitive"}],
                   "irregular_forms":{"base":"come","past":"came","past_participle":"come"}},
                  {"translation":"стать (делать что-то)","surface_form":"come to <verb>",
                   "unit_type":"irregular_verb","base_lemma":"come",
                   "explanation":"постепенно начать что-то делать",
                   "examples":[{"sentence":"I [[came to]] like it."}],
                   "complementation":["to_infinitive"],
                   "irregular_forms":{"base":"come","past":"came","past_participle":"come"}}
                ]}
                """.trimIndent(),
            )

        assertEquals(2, result.suggestions.size)
        val forms = result.suggestions.first().irregularForms
        assertEquals(IrregularFormsHint("come", "came", "come"), forms)
        assertEquals("come to <verb>", result.suggestions[1].surfaceForm)
        assertEquals(listOf("to_infinitive"), result.suggestions[1].complementation)
    }

    @Test
    fun `look - look at look after look for are separate senses not alternatives`() {
        val result =
            map(
                """
                {"version": 1,"items":[
                  {"translation":"смотреть на","surface_form":"look at","unit_type":"phrasal_verb",
                   "base_lemma":"look","explanation":"направлять взгляд на что-то",
                   "examples":[{"sentence":"[[Look at]] this."}]},
                  {"translation":"присматривать за","surface_form":"look after",
                   "unit_type":"phrasal_verb","base_lemma":"look",
                   "explanation":"заботиться о ком-то","examples":[{"sentence":"She [[looks after]] him."}]},
                  {"translation":"искать","surface_form":"look for","unit_type":"phrasal_verb",
                   "base_lemma":"look","explanation":"пытаться найти",
                   "examples":[{"sentence":"I'm [[looking for]] keys."}]}
                ]}
                """.trimIndent(),
            )

        assertEquals(
            listOf("look at", "look after", "look for"),
            result.suggestions.map { it.surfaceForm },
        )
        assertTrue(result.suggestions.all { it.governedPrepositions.isEmpty() })
    }

    @Test
    fun `look down on - phrasal-prepositional verb keeps fixed parts and inseparable tag`() {
        val result =
            map(
                """
                {"version": 1,"items":[
                  {"translation":"презирать","surface_form":"look down on <someone>",
                   "unit_type":"phrasal_verb","base_lemma":"look",
                   "explanation":"считать кого-то ниже себя, презирать",
                   "examples":[{"sentence":"They [[look down on]] outsiders."}],
                   "grammar_tags":[{"category":"separability","form":"inseparable"}]}
                ]}
                """.trimIndent(),
            )

        val sense = result.suggestions.single()
        assertEquals("look down on <someone>", sense.surfaceForm)
        assertEquals(GrammarTagHint("separability", "inseparable"), sense.grammarTags.single())
        assertTrue(sense.governedPrepositions.isEmpty())
    }

    @Test
    fun `come across - one sense for a person or a thing with a generic slot in a single span`() {
        val result =
            map(
                """
                {"version": 1,"items":[
                  {"translation":"наткнуться, случайно встретить",
                   "surface_form":"come across <someone/something>",
                   "unit_type":"phrasal_verb","base_lemma":"come",
                   "explanation":"случайно найти или встретить",
                   "examples":[{"sentence":"She [[came across]] old letters."},
                               {"sentence":"I [[came across]] an old friend."}]}
                ]}
                """.trimIndent(),
            )

        val sense = result.suggestions.single()
        assertEquals("come across <someone/something>", sense.surfaceForm)
        assertEquals(
            listOf("She [[came across]] old letters.", "I [[came across]] an old friend."),
            sense.examples.map { it.sentence },
        )
    }

    @Test
    fun `come across - the three common senses pass through as distinct items`() {
        val result =
            map(
                """
                {"version": 1,"items":[
                  {"translation":"случайно найти","surface_form":"come across <someone/something>",
                   "unit_type":"phrasal_verb","base_lemma":"come",
                   "explanation":"случайно найти или встретить",
                   "examples":[{"sentence":"I [[came across]] an old photo."}]},
                  {"translation":"производить впечатление","surface_form":"come across [as]",
                   "unit_type":"phrasal_verb","base_lemma":"come",
                   "explanation":"казаться, восприниматься определённым образом",
                   "examples":[{"sentence":"He [[comes across]] as confident."}]},
                  {"translation":"быть понятно выраженным","surface_form":"come across",
                   "unit_type":"phrasal_verb","base_lemma":"come",
                   "explanation":"быть ясно донесённым до аудитории",
                   "examples":[{"sentence":"Her message [[came across]] clearly."}]}
                ]}
                """.trimIndent(),
            )

        assertEquals(
            listOf("случайно найти", "производить впечатление", "быть понятно выраженным"),
            result.suggestions.map { it.translation },
        )
        assertEquals(
            listOf("come across <someone/something>", "come across [as]", "come across"),
            result.suggestions.map { it.surfaceForm },
        )
        assertTrue(
            result.suggestions.all {
                it.examples
                    .single()
                    .sentence
                    .contains("[[")
            },
        )
    }

    @Test
    fun `a piece of cake - idiom is a fixed expression with fixed parts in surface_form`() {
        val result =
            map(
                """
                {"version": 1,"items":[
                  {"translation":"проще простого","surface_form":"a piece of cake",
                   "unit_type":"idiom","explanation":"что-то очень лёгкое",
                   "examples":[{"sentence":"The exam was [[a piece of cake]]."}],
                   "grammar_tags":[{"category":"expression_type","form":"fixed"}]}
                ]}
                """.trimIndent(),
            )

        val sense = result.suggestions.single()
        assertEquals("a piece of cake", sense.surfaceForm)
        assertEquals("idiom", sense.unitType)
        assertEquals(GrammarTagHint("expression_type", "fixed"), sense.grammarTags.single())
    }

    @Test
    fun `in order to - phrase exposes a bare-infinitive slot`() {
        val result =
            map(
                """
                {"version": 1,"items":[
                  {"translation":"чтобы","surface_form":"in order to <verb>","unit_type":"phrase",
                   "explanation":"выражает цель действия",
                   "examples":[{"sentence":"He left early [[in order to]] catch the train."}],
                   "complementation":["bare_infinitive"]}
                ]}
                """.trimIndent(),
            )

        val sense = result.suggestions.single()
        assertEquals("in order to <verb>", sense.surfaceForm)
        assertEquals(listOf("bare_infinitive"), sense.complementation)
    }

    @Test
    fun `interested in - adjective with governed preposition and not a phrasal verb`() {
        val result =
            map(
                """
                {"version": 1,"items":[
                  {"translation":"заинтересованный","surface_form":"interested",
                   "unit_type":"adjective","base_lemma":"interested",
                   "explanation":"проявляющий интерес к чему-то",
                   "examples":[{"sentence":"She is [[interested]] in art."}],
                   "preposition_government":[{"alternatives":["in"]}]}
                ]}
                """.trimIndent(),
            )

        val sense = result.suggestions.single()
        assertEquals("adjective", sense.unitType)
        assertEquals(listOf("in"), sense.governedPrepositions.single().alternatives)
        assertNull(sense.governedPrepositions.single().example)
    }
}
