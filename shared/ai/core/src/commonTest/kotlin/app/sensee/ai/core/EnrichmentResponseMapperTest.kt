package app.sensee.ai.core

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
                            headLemma = "come",
                            components =
                                listOf(
                                    UnitComponentDtoV1("come", "head"),
                                    UnitComponentDtoV1("across", "particle"),
                                ),
                            explanation = "to give a particular impression",
                            examples =
                                listOf(
                                    EnrichmentExampleV1(sentence = "She comes across as shy."),
                                    EnrichmentExampleV1(sentence = "He came across well."),
                                ),
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
        assertEquals("come", suggestion.headLemma)
        assertEquals(
            listOf(UnitComponentHint("come", "head"), UnitComponentHint("across", "particle")),
            suggestion.components,
        )
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
    fun `synonyms antonyms and collocations map through with blanks dropped`() {
        val response =
            EnrichmentResponseV1(
                items =
                    listOf(
                        EnrichmentItemV1(
                            translation = "наткнуться",
                            synonyms = listOf("encounter", "  ", "stumble upon"),
                            antonyms = listOf("avoid"),
                            collocations = listOf("come across as", "   "),
                        ),
                    ),
            )

        val suggestion = EnrichmentResponseMapper.map(response).suggestions.single()

        assertEquals(listOf("encounter", "stumble upon"), suggestion.synonyms)
        assertEquals(listOf("avoid"), suggestion.antonyms)
        assertEquals(listOf("come across as"), suggestion.collocations)
    }

    @Test
    fun `word family derivatives map through with incomplete entries dropped`() {
        val response =
            EnrichmentResponseV1(
                items =
                    listOf(
                        EnrichmentItemV1(
                            translation = "решать",
                            baseLemma = "decide",
                            wordFamily =
                                listOf(
                                    WordFamilyEntryDtoV1("decision", "noun"),
                                    WordFamilyEntryDtoV1("  ", "adjective"),
                                    WordFamilyEntryDtoV1("decisive", "  "),
                                    WordFamilyEntryDtoV1("decisively", "adverb"),
                                ),
                        ),
                    ),
            )

        val suggestion = EnrichmentResponseMapper.map(response).suggestions.single()

        assertEquals(
            listOf(WordFamilyHint("decision", "noun"), WordFamilyHint("decisively", "adverb")),
            suggestion.wordFamily,
        )
    }

    @Test
    fun `component salience is carried through the mapper`() {
        val response =
            EnrichmentResponseV1(
                items =
                    listOf(
                        EnrichmentItemV1(
                            translation = "наткнуться",
                            components =
                                listOf(
                                    UnitComponentDtoV1("come", "head", "primary"),
                                    UnitComponentDtoV1("across", "particle", "secondary"),
                                ),
                        ),
                    ),
            )

        val components =
            EnrichmentResponseMapper
                .map(response)
                .suggestions
                .single()
                .components

        assertEquals(
            listOf(
                UnitComponentHint("come", "head", "primary"),
                UnitComponentHint("across", "particle", "secondary"),
            ),
            components,
        )
    }

    @Test
    fun `a component with blank text or role is dropped not constructed`() {
        val response =
            EnrichmentResponseV1(
                items =
                    listOf(
                        EnrichmentItemV1(
                            translation = "наткнуться",
                            components =
                                listOf(
                                    UnitComponentDtoV1("come", "head"),
                                    UnitComponentDtoV1("", "particle"),
                                    UnitComponentDtoV1("across", "  "),
                                ),
                        ),
                    ),
            )

        val suggestion = EnrichmentResponseMapper.map(response).suggestions.single()

        assertEquals(listOf(UnitComponentHint("come", "head")), suggestion.components)
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
    fun `a sentence-only example normalises into a bare EnrichmentExample`() {
        val response =
            EnrichmentResponseV1(
                items =
                    listOf(
                        EnrichmentItemV1(
                            translation = "идти",
                            examples = listOf(EnrichmentExampleV1(sentence = "She [[walks]] to school.")),
                        ),
                    ),
            )

        val example =
            EnrichmentResponseMapper
                .map(response)
                .suggestions
                .single()
                .examples
                .single()

        assertEquals("She [[walks]] to school.", example.sentence)
        assertNull(example.translation)
        assertEquals(emptyList(), example.alignment)
    }

    @Test
    fun `a structured example carries its translation and alignment through the mapper`() {
        val structured =
            EnrichmentExampleV1(
                sentence = "She [[came across]] an old photo.",
                translation = "Она наткнулась на старую фотографию.",
                alignment =
                    listOf(
                        AlignmentChunkV1(source = "She", target = "Она"),
                        AlignmentChunkV1(source = "came across", target = "наткнулась на"),
                        AlignmentChunkV1(source = "an old photo", target = "старую фотографию"),
                    ),
            )
        val response =
            EnrichmentResponseV1(
                items = listOf(EnrichmentItemV1(translation = "наткнуться", examples = listOf(structured))),
            )

        val example =
            EnrichmentResponseMapper
                .map(response)
                .suggestions
                .single()
                .examples
                .single()

        assertEquals("She [[came across]] an old photo.", example.sentence)
        assertEquals("Она наткнулась на старую фотографию.", example.translation)
        assertEquals(3, example.alignment.size)
        assertEquals(AlignmentChunk("came across", "наткнулась на"), example.alignment[1])
    }

    @Test
    fun `an example with a blank sentence is dropped not crash the mapping`() {
        val response =
            EnrichmentResponseV1(
                items =
                    listOf(
                        EnrichmentItemV1(
                            translation = "идти",
                            examples =
                                listOf(
                                    EnrichmentExampleV1(sentence = "   "),
                                    EnrichmentExampleV1(sentence = "She [[walks]] to school."),
                                ),
                        ),
                    ),
            )

        val examples =
            EnrichmentResponseMapper
                .map(response)
                .suggestions
                .single()
                .examples

        assertEquals(1, examples.size)
        assertEquals("She [[walks]] to school.", examples.single().sentence)
    }

    @Test
    fun `unsupported schema version yields no suggestions`() {
        val result = EnrichmentResponseMapper.map(EnrichmentResponseV1(version = 99))

        assertTrue(result.availability is EnrichmentAvailability.Degraded)
        assertEquals(emptyList(), result.suggestions)
    }
}
