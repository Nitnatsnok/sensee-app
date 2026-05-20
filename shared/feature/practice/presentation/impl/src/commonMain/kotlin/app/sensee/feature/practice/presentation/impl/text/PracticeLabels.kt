package app.sensee.feature.practice.presentation.impl.text

import app.sensee.core.presentation.text.TextProvider
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType

internal fun GrammarUnitType.label(textProvider: TextProvider = DefaultPracticeTextProvider): String =
    textProvider.text(PracticeTextKeys.unitType(this))

internal fun GrammarTag.label(textProvider: TextProvider = DefaultPracticeTextProvider): String =
    textProvider.text(PracticeTextKeys.grammarForm(form))

internal fun GrammarUnitType.description(textProvider: TextProvider = DefaultPracticeTextProvider): String =
    textProvider.text(PracticeTextKeys.unitTypeDescription(this))

internal fun GrammarForm.description(textProvider: TextProvider = DefaultPracticeTextProvider): String =
    textProvider.text(PracticeTextKeys.grammarFormDescription(this))
