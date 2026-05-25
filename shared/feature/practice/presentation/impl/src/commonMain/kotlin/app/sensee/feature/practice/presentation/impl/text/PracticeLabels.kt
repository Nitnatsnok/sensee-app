package app.sensee.feature.practice.presentation.impl.text

import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarLabelForm
import app.sensee.grammar.domain.GrammarLabelStyle
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType

/**
 * Practice-card label helpers over the shared [GrammarLabels] dictionary.
 * Uses the textbook-style [GrammarLabelStyle.Pedagogical] short forms
 * (`vt.`/`vi.`/`V1`/`V2`/`V3`/`count.`/`uncount.`). Fallback to the raw `id`
 * covers both missing-language and `Unknown(id)` (ADR-006) — surfaces a
 * fresh backend taxonomy value instead of dropping it.
 */
internal fun GrammarUnitType.shortLabel(
    labels: GrammarLabels,
    language: String,
): String =
    labels.unitType(this, language, GrammarLabelForm.Short, GrammarLabelStyle.Pedagogical)
        ?: id

internal fun GrammarTag.shortLabel(
    labels: GrammarLabels,
    language: String,
): String =
    labels.form(this, language, GrammarLabelForm.Short, GrammarLabelStyle.Pedagogical)
        ?: form.id

internal fun GrammarForm.shortLabel(
    labels: GrammarLabels,
    language: String,
): String =
    labels.formByName(this, language, GrammarLabelForm.Short, GrammarLabelStyle.Pedagogical)
        ?: id

internal fun GrammarUnitType.description(
    labels: GrammarLabels,
    language: String,
): String = labels.unitType(this, language, GrammarLabelForm.Long) ?: id

internal fun GrammarForm.description(
    labels: GrammarLabels,
    language: String,
): String = labels.formByName(this, language, GrammarLabelForm.Long) ?: id
