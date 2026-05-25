package app.sensee.feature.vocabularyEditor.data

import app.sensee.feature.vocabularyEditor.domain.ContextualApplication
import app.sensee.feature.vocabularyEditor.domain.Meaning
import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.PrepositionGovernment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.grammar.domain.UsageAxis
import app.sensee.grammar.domain.UsageLabel
import app.sensee.grammar.domain.UsageValue
import kotlin.test.Test
import kotlin.test.assertEquals

class MeaningDtoRoundTripTest {
    @Test
    fun `a fully populated meaning survives a toDto - toDomain round trip`() {
        val original =
            Meaning(
                translation = "наткнуться",
                surfaceForm = SurfaceForm.parse("come across <something>"),
                unitType = GrammarUnitType.PhrasalVerb,
                baseLemma = "come",
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
            )

        val restored = original.toDto().toDomain()

        assertEquals(original, restored)
    }

    @Test
    fun `an unknown unit type id read from storage surfaces as Unknown not crash`() {
        val dto = MeaningDto(translation = "x", unitType = "future_tense_marker")

        val restored = dto.toDomain()

        assertEquals(GrammarUnitType.Unknown("future_tense_marker"), restored.unitType)
    }

    @Test
    fun `an unknown grammar tag pair read from storage stays as Unknown branches`() {
        val dto =
            MeaningDto(
                translation = "x",
                grammarTags = listOf(GrammarTagDto(category = "novel_axis", form = "novel_form")),
            )

        val tag = dto.toDomain().grammarTags.single()

        assertEquals(GrammarCategory.Unknown("novel_axis"), tag.category)
        assertEquals(GrammarForm.Unknown("novel_form"), tag.form)
    }

    @Test
    fun `unit type is written with its neutral id not the data object name`() {
        val dto = Meaning(translation = "x", unitType = GrammarUnitType.IrregularVerb).toDto()

        assertEquals("irregular_verb", dto.unitType)
    }
}
