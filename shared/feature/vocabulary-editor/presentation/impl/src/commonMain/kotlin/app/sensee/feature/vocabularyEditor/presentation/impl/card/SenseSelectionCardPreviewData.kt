package app.sensee.feature.vocabularyEditor.presentation.impl.card

import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.PrepositionGovernment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.lexicon.domain.ContextualApplication
import app.sensee.lexicon.domain.Sense

/**
 * Sample [Sense]s for [SenseSelectionCard] previews — a rich phrasal verb (every
 * detail field populated), an irregular verb, and a minimal sense with no
 * secondary detail.
 */
internal object SenseSelectionCardPreviewData {
    val richPhrasalVerb: Sense =
        Sense(
            translation = "производить впечатление",
            surfaceForm = SurfaceForm.parse("come across [as]"),
            unitType = GrammarUnitType.PhrasalVerb,
            baseLemma = "come",
            explanation = "казаться обладающим неким качеством",
            contextualApplications =
                listOf(
                    ContextualApplication(
                        StudiedSentence.parse("She [[comes across]] [[as]] very confident."),
                    ),
                    ContextualApplication(
                        StudiedSentence.parse("He [[came across]] really well in the interview."),
                    ),
                ),
            governedPrepositions = listOf(PrepositionGovernment(listOf("as"))),
            complementation = listOf(ComplementType.Adjective, ComplementType.Noun),
            usageNote = "обычно о впечатлении, которое человек производит на других",
            grammarTags =
                listOf(
                    GrammarTag(GrammarCategory.Separability, GrammarForm.Inseparable),
                    GrammarTag(GrammarCategory.Transitivity, GrammarForm.Intransitive),
                ),
            irregularForms = IrregularForms("come", "came", "come"),
        )

    val irregularVerb: Sense =
        Sense(
            translation = "бежать",
            surfaceForm = SurfaceForm.parse("run"),
            unitType = GrammarUnitType.IrregularVerb,
            baseLemma = "run",
            explanation = "двигаться быстро, перебирая ногами",
            contextualApplications =
                listOf(
                    ContextualApplication(
                        StudiedSentence.parse("She had to [[run]] to catch the last train."),
                    ),
                    ContextualApplication(StudiedSentence.parse("They [[ran]] across the field.")),
                ),
            complementation = listOf(ComplementType.Intransitive),
            grammarTags = listOf(GrammarTag(GrammarCategory.Transitivity, GrammarForm.Intransitive)),
            irregularForms = IrregularForms("run", "ran", "run"),
        )

    val minimal: Sense =
        Sense(
            translation = "наткнуться",
            surfaceForm = SurfaceForm.parse("come across <something>"),
            unitType = GrammarUnitType.PhrasalVerb,
            explanation = "случайно обнаружить что-либо",
            contextualApplications =
                listOf(ContextualApplication(StudiedSentence.parse("I [[came across]] an old photo."))),
        )
}
