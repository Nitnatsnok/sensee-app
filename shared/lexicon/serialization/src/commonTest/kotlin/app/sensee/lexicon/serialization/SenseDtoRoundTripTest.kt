package app.sensee.lexicon.serialization

import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.ComponentRole
import app.sensee.grammar.domain.ComponentSalience
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.PrepositionGovernment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.grammar.domain.UnitComponent
import app.sensee.grammar.domain.UsageAxis
import app.sensee.grammar.domain.UsageLabel
import app.sensee.grammar.domain.UsageValue
import app.sensee.lexicon.domain.AlignmentChunk
import app.sensee.lexicon.domain.ContextualApplication
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.WordFamilyMember
import kotlin.test.Test
import kotlin.test.assertEquals

class SenseDtoRoundTripTest {
    @Test
    fun `a fully populated sense survives a toDto - toDomain round trip`() {
        val original =
            Sense(
                translation = "наткнуться",
                surfaceForm = SurfaceForm.parse("come across <something>"),
                unitType = GrammarUnitType.PhrasalVerb,
                baseLemma = "come",
                headLemma = "come",
                components =
                    listOf(
                        UnitComponent("come", ComponentRole.Head, ComponentSalience.Primary),
                        UnitComponent("across", ComponentRole.Particle, ComponentSalience.Secondary),
                    ),
                explanation = "случайно обнаружить что-либо",
                contextualApplications =
                    listOf(
                        ContextualApplication(StudiedSentence.parse("I [[came across]] an old photo.")),
                    ),
                governedPrepositions =
                    listOf(
                        PrepositionGovernment(listOf("as"), example = "comes across as confident"),
                    ),
                complementation = listOf(ComplementType.Noun, ComplementType.Intransitive),
                usageLabels = listOf(UsageLabel(UsageAxis.Register, UsageValue.Informal)),
                usageNote = "обычно об ощущениях",
                grammarTags =
                    listOf(
                        GrammarTag(GrammarCategory.VerbIrregular, GrammarForm.Infinitive),
                        GrammarTag(GrammarCategory.Separability, GrammarForm.Inseparable),
                    ),
                irregularForms = IrregularForms("come", "came", "come"),
                synonyms = listOf("encounter", "stumble upon"),
                antonyms = listOf("avoid"),
                collocations = listOf("come across a problem"),
                wordFamily = listOf(WordFamilyMember("comer", GrammarUnitType.Noun)),
            )

        val restored = original.toDto().toDomain()

        assertEquals(original, restored)
    }

    @Test
    fun `an unknown component salience read from storage surfaces as Unknown not crash`() {
        val dto =
            SenseDto(
                translation = "x",
                components = listOf(UnitComponentDto(text = "come", role = "head", salience = "future_level")),
            )

        val restored = dto.toDomain()

        assertEquals(ComponentSalience.Unknown("future_level"), restored.components.single().salience)
    }

    @Test
    fun `headLemma and components default to absent on a thin draft sense`() {
        val original = Sense(translation = "x", unitType = GrammarUnitType.Verb)

        val restored = original.toDto().toDomain()

        assertEquals(null, restored.headLemma)
        assertEquals(emptyList(), restored.components)
    }

    @Test
    fun `an unknown component role read from storage surfaces as Unknown not crash`() {
        val dto =
            SenseDto(
                translation = "x",
                components = listOf(UnitComponentDto(text = "of", role = "future_role")),
            )

        val restored = dto.toDomain()

        assertEquals(
            UnitComponent("of", ComponentRole.Unknown("future_role")),
            restored.components.single(),
        )
    }

    @Test
    fun `an unknown unit type id read from storage surfaces as Unknown not crash`() {
        val dto = SenseDto(translation = "x", unitType = "future_tense_marker")

        val restored = dto.toDomain()

        assertEquals(GrammarUnitType.Unknown("future_tense_marker"), restored.unitType)
    }

    @Test
    fun `an unknown grammar tag pair read from storage stays as Unknown branches`() {
        val dto =
            SenseDto(
                translation = "x",
                grammarTags = listOf(GrammarTagDto(category = "novel_axis", form = "novel_form")),
            )

        val tag = dto.toDomain().grammarTags.single()

        assertEquals(GrammarCategory.Unknown("novel_axis"), tag.category)
        assertEquals(GrammarForm.Unknown("novel_form"), tag.form)
    }

    @Test
    fun `unit type is written with its neutral id not the data object name`() {
        val dto = Sense(translation = "x", unitType = GrammarUnitType.IrregularVerb).toDto()

        assertEquals("irregular_verb", dto.unitType)
    }

    @Test
    fun `contextual application translation and alignment survive a round trip`() {
        val original =
            Sense(
                translation = "наткнуться",
                contextualApplications =
                    listOf(
                        ContextualApplication(
                            sentence = StudiedSentence.parse("I [[came across]] an old photo."),
                            translation = "Я наткнулся на старое фото.",
                            alignment =
                                listOf(
                                    AlignmentChunk(source = "came across", target = "наткнулся"),
                                    AlignmentChunk(source = "photo", target = "фото"),
                                ),
                        ),
                    ),
            )

        val restored = original.toDto().toDomain()

        assertEquals(original, restored)
    }
}
