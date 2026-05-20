package app.sensee.grammar.domain

public enum class GrammarForm(
    public val id: String,
) {
    Singular("singular"),
    Plural("plural"),
    CommonCase("common_case"),
    NounPossessiveCase("possessive_case"),
    Countable("countable"),
    Uncountable("uncountable"),
    Infinitive("infinitive"),
    PastTense("past_tense"),
    PastParticiple("past_participle"),
    PresentParticiple("present_participle"),
    Gerund("gerund"),
    Transitive("transitive"),
    Intransitive("intransitive"),
    Separable("separable"),
    Inseparable("inseparable"),
    Positive("positive"),
    Comparative("comparative"),
    Superlative("superlative"),
    Subjective("subjective"),
    Objective("objective"),
    PronounPossessive("possessive_pronoun"),
    Reflexive("reflexive"),
    Demonstrative("demonstrative"),
    Quantifier("quantifier"),
    Cardinal("cardinal"),
    Ordinal("ordinal"),
    Definite("definite"),
    Indefinite("indefinite"),
    Invariant("invariant"),
    Fixed("fixed"),
    ;

    public companion object {
        public fun fromId(id: String): GrammarForm? = entries.firstOrNull { it.id == id }
    }
}

private val VERB_FORMS: Set<GrammarForm> =
    setOf(
        GrammarForm.Infinitive,
        GrammarForm.PastTense,
        GrammarForm.PastParticiple,
        GrammarForm.PresentParticiple,
        GrammarForm.Gerund,
    )

/**
 * Grammar categories and the closed set of forms each admits. The
 * category -> allowed-forms invariant is canonical in `docs/pos-and-forms.adoc`
 * (ADR-001): a form belongs to exactly one category and a pair outside the
 * table is invalid. [Verb] forms are rule-derived; [VerbIrregular] additionally
 * carries lexically stored principal parts.
 */
public enum class GrammarCategory(
    public val id: String,
    public val allowedForms: Set<GrammarForm>,
) {
    Number("number", setOf(GrammarForm.Singular, GrammarForm.Plural)),
    Case("case", setOf(GrammarForm.CommonCase, GrammarForm.NounPossessiveCase)),
    Countability("countability", setOf(GrammarForm.Countable, GrammarForm.Uncountable)),
    Verb("verb", VERB_FORMS),
    VerbIrregular("verb_irregular", VERB_FORMS),
    Transitivity("transitivity", setOf(GrammarForm.Transitive, GrammarForm.Intransitive)),
    Separability("separability", setOf(GrammarForm.Separable, GrammarForm.Inseparable)),
    Degree("degree", setOf(GrammarForm.Positive, GrammarForm.Comparative, GrammarForm.Superlative)),
    PronounType(
        "pronoun_type",
        setOf(
            GrammarForm.Subjective,
            GrammarForm.Objective,
            GrammarForm.PronounPossessive,
            GrammarForm.Reflexive,
        ),
    ),
    DeterminerType("determiner_type", setOf(GrammarForm.Demonstrative, GrammarForm.Quantifier)),
    NumeralType("numeral_type", setOf(GrammarForm.Cardinal, GrammarForm.Ordinal)),
    Definiteness("definiteness", setOf(GrammarForm.Definite, GrammarForm.Indefinite)),
    Invariance("invariance", setOf(GrammarForm.Invariant)),
    ExpressionType("expression_type", setOf(GrammarForm.Fixed)),
    ;

    public companion object {
        public fun fromId(id: String): GrammarCategory? = entries.firstOrNull { it.id == id }
    }
}

/**
 * A single grammatical feature of a sense/card. Enforces the
 * category -> form invariant at construction so an invalid pair can never
 * exist in the model (ADR-001: validate, do not trust the caller).
 */
public data class GrammarTag(
    val category: GrammarCategory,
    val form: GrammarForm,
) {
    init {
        require(form in category.allowedForms) {
            "Grammar form ${form.id} is not valid for category ${category.id}"
        }
    }

    public companion object {
        /**
         * Resolves a (categoryId, formId) pair, returning null when either id is
         * unknown or the pair violates the category -> form invariant. Used at
         * the AI boundary so a malformed hint is dropped, never constructed.
         */
        public fun resolve(
            categoryId: String,
            formId: String,
        ): GrammarTag? {
            val category = GrammarCategory.fromId(categoryId) ?: return null
            val form = GrammarForm.fromId(formId) ?: return null
            return if (form in category.allowedForms) GrammarTag(category, form) else null
        }
    }
}
