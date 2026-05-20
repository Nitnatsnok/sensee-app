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
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class MeaningDtoTest {
    @Test
    fun `the rich sense model round-trips through the persisted DTO json`() {
        val meaning =
            Meaning(
                translation = "произвести впечатление",
                surfaceForm = SurfaceForm.parse("come across [as]"),
                unitType = GrammarUnitType.PhrasalVerb,
                baseLemma = "come",
                explanation = "to give a particular impression",
                contextualApplications =
                    listOf(ContextualApplication(StudiedSentence.parse("She [[comes across]] as shy."))),
                governedPrepositions = listOf(PrepositionGovernment(listOf("as"), "comes across as shy")),
                complementation = listOf(ComplementType.PrepositionalPhrase),
                usageLabels = listOf(UsageLabel(UsageAxis.Register, UsageValue.Informal)),
                usageNote = "of impressions, not objects",
                grammarTags = listOf(GrammarTag(GrammarCategory.VerbIrregular, GrammarForm.Infinitive)),
                irregularForms = IrregularForms("come", "came", "come"),
            )

        val json = Json.encodeToString(listOf(meaning.toDto()))
        val restored = Json.decodeFromString<List<MeaningDto>>(json).single().toDomain()

        assertEquals("произвести впечатление", restored.translation)
        assertEquals("come across [as]", restored.surfaceForm?.display())
        assertEquals(GrammarUnitType.PhrasalVerb, restored.unitType)
        assertEquals("come", restored.baseLemma)
        val sentence = restored.contextualApplications.single().sentence
        assertEquals("She comes across as shy.", sentence.plainText())
        assertEquals("comes across", sentence.target)
        assertEquals(
            listOf(PrepositionGovernment(listOf("as"), "comes across as shy")),
            restored.governedPrepositions,
        )
        assertEquals(listOf(ComplementType.PrepositionalPhrase), restored.complementation)
        assertEquals(listOf(UsageLabel(UsageAxis.Register, UsageValue.Informal)), restored.usageLabels)
        assertEquals("of impressions, not objects", restored.usageNote)
        assertEquals(
            listOf(GrammarTag(GrammarCategory.VerbIrregular, GrammarForm.Infinitive)),
            restored.grammarTags,
        )
        assertEquals(IrregularForms("come", "came", "come"), restored.irregularForms)
    }
}
