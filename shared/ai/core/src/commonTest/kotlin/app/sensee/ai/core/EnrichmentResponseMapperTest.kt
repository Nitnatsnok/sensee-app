package app.sensee.ai.core

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnrichmentResponseMapperTest {
    @Test
    fun `item without a usable translation is dropped and result degrades`() {
        val response =
            EnrichmentResponseV1(
                items =
                    listOf(
                        EnrichmentItemV1(translation = "идти", explanation = "to move on foot"),
                        EnrichmentItemV1(translation = "  ", explanation = "incomplete candidate"),
                    ),
            )

        val result = EnrichmentResponseMapper.map(response)

        assertEquals(listOf("идти"), result.suggestions.map { it.translation })
        assertTrue(result.availability is EnrichmentAvailability.Degraded)
    }

    @Test
    fun `the structured sense shape passes through the seam neutrally`() {
        val response =
            EnrichmentResponseV1(
                items =
                    listOf(
                        EnrichmentItemV1(
                            translation = "произвести впечатление",
                            surfaceForm = "come across [as]",
                            unitType = "phrasal_verb",
                            baseLemma = "come",
                            explanation = "to give a particular impression",
                            examples = listOf("She comes across as shy.", "He came across well."),
                            prepositionGovernment =
                                listOf(PrepositionGovernmentDtoV1(listOf("as"), "comes across as shy")),
                            complementation = listOf("prepositional_phrase"),
                            usageLabels = listOf(UsageLabelDtoV1("register", "informal")),
                            usageNote = "of impressions, not objects",
                            grammarTags = listOf(GrammarTagDtoV1("verb_irregular", "infinitive")),
                            irregularForms = IrregularFormsDtoV1("come", "came", "come"),
                        ),
                    ),
            )

        val suggestion = EnrichmentResponseMapper.map(response).suggestions.single()

        assertEquals("come across [as]", suggestion.surfaceForm)
        assertEquals("phrasal_verb", suggestion.unitType)
        assertEquals("come", suggestion.baseLemma)
        assertEquals(2, suggestion.examples.size)
        assertEquals(
            listOf(PrepositionGovernmentHint(listOf("as"), "comes across as shy")),
            suggestion.governedPrepositions,
        )
        assertEquals(listOf("prepositional_phrase"), suggestion.complementation)
        assertEquals(listOf(UsageLabelHint("register", "informal")), suggestion.usageLabels)
        assertEquals("of impressions, not objects", suggestion.usageNote)
        assertEquals(GrammarTagHint("verb_irregular", "infinitive"), suggestion.grammarTags.single())
        assertEquals(IrregularFormsHint("come", "came", "come"), suggestion.irregularForms)
    }

    @Test
    fun `extension values are carried by item index`() {
        val response =
            EnrichmentResponseV1(
                items =
                    listOf(
                        EnrichmentItemV1(translation = "идти"),
                        EnrichmentItemV1(translation = "работать"),
                    ),
            )
        val extensions =
            listOf(
                mapOf("etymology" to JsonPrimitive("Old English")),
                mapOf("etymology" to JsonPrimitive("Norse")),
            )

        val suggestions = EnrichmentResponseMapper.map(response, extensions).suggestions

        assertEquals(JsonPrimitive("Old English"), suggestions[0].extensions["etymology"])
        assertEquals(JsonPrimitive("Norse"), suggestions[1].extensions["etymology"])
    }

    @Test
    fun `unsupported schema version yields no suggestions`() {
        val result = EnrichmentResponseMapper.map(EnrichmentResponseV1(version = 99))

        assertTrue(result.availability is EnrichmentAvailability.Degraded)
        assertEquals(emptyList(), result.suggestions)
    }
}
