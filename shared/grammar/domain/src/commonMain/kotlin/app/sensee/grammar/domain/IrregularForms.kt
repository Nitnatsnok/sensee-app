package app.sensee.grammar.domain

/**
 * The lexically stored principal parts of an irregular verb (ADR-001): base,
 * past tense and past participle (e.g. come / came / come). For a unit of
 * [GrammarUnitType.IrregularVerb] these back the related form-variant cards;
 * they are stored, not rule-derived (the [GrammarCategory.VerbIrregular]
 * obligation in `docs/domain/pos-and-forms.adoc`).
 */
public data class IrregularForms(
    val base: String,
    val past: String,
    val pastParticiple: String,
)
