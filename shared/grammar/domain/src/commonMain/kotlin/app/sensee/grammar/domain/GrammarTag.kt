package app.sensee.grammar.domain

/**
 * A grammatical form value (e.g. `infinitive`, `plural`, `inseparable`).
 * Sealed; allowed forms per category live in
 * [TaxonomyInvariants.allowedFormsByCategory] — runtime, not a closed enum set.
 */
public sealed interface GrammarForm {
    public val id: String

    public data object Singular : GrammarForm {
        override val id: String = "singular"
    }

    public data object Plural : GrammarForm {
        override val id: String = "plural"
    }

    public data object NounPossessiveCase : GrammarForm {
        override val id: String = "possessive_case"
    }

    public data object Countable : GrammarForm {
        override val id: String = "countable"
    }

    public data object Uncountable : GrammarForm {
        override val id: String = "uncountable"
    }

    public data object Infinitive : GrammarForm {
        override val id: String = "infinitive"
    }

    public data object PastTense : GrammarForm {
        override val id: String = "past_tense"
    }

    public data object PastParticiple : GrammarForm {
        override val id: String = "past_participle"
    }

    public data object PresentParticiple : GrammarForm {
        override val id: String = "present_participle"
    }

    public data object Gerund : GrammarForm {
        override val id: String = "gerund"
    }

    public data object Transitive : GrammarForm {
        override val id: String = "transitive"
    }

    public data object Intransitive : GrammarForm {
        override val id: String = "intransitive"
    }

    public data object Separable : GrammarForm {
        override val id: String = "separable"
    }

    public data object Inseparable : GrammarForm {
        override val id: String = "inseparable"
    }

    public data object Positive : GrammarForm {
        override val id: String = "positive"
    }

    public data object Comparative : GrammarForm {
        override val id: String = "comparative"
    }

    public data object Superlative : GrammarForm {
        override val id: String = "superlative"
    }

    public data object Subjective : GrammarForm {
        override val id: String = "subjective"
    }

    public data object Objective : GrammarForm {
        override val id: String = "objective"
    }

    public data object PronounPossessive : GrammarForm {
        override val id: String = "possessive_pronoun"
    }

    public data object Reflexive : GrammarForm {
        override val id: String = "reflexive"
    }

    public data object Demonstrative : GrammarForm {
        override val id: String = "demonstrative"
    }

    public data object Quantifier : GrammarForm {
        override val id: String = "quantifier"
    }

    public data object PossessiveDeterminer : GrammarForm {
        override val id: String = "possessive_determiner"
    }

    public data object Distributive : GrammarForm {
        override val id: String = "distributive"
    }

    public data object Interrogative : GrammarForm {
        override val id: String = "interrogative"
    }

    public data object Cardinal : GrammarForm {
        override val id: String = "cardinal"
    }

    public data object Ordinal : GrammarForm {
        override val id: String = "ordinal"
    }

    public data object Definite : GrammarForm {
        override val id: String = "definite"
    }

    public data object Indefinite : GrammarForm {
        override val id: String = "indefinite"
    }

    public data object Invariant : GrammarForm {
        override val id: String = "invariant"
    }

    public data object Coordinating : GrammarForm {
        override val id: String = "coordinating"
    }

    public data object Subordinating : GrammarForm {
        override val id: String = "subordinating"
    }

    public data object Correlative : GrammarForm {
        override val id: String = "correlative"
    }

    public data object Fixed : GrammarForm {
        override val id: String = "fixed"
    }

    public data class Unknown(
        override val id: String,
    ) : GrammarForm

    public companion object {
        public val knownEntries: List<GrammarForm> =
            listOf(
                Singular,
                Plural,
                NounPossessiveCase,
                Countable,
                Uncountable,
                Infinitive,
                PastTense,
                PastParticiple,
                PresentParticiple,
                Gerund,
                Transitive,
                Intransitive,
                Separable,
                Inseparable,
                Positive,
                Comparative,
                Superlative,
                Subjective,
                Objective,
                PronounPossessive,
                Reflexive,
                Demonstrative,
                Quantifier,
                PossessiveDeterminer,
                Distributive,
                Interrogative,
                Cardinal,
                Ordinal,
                Definite,
                Indefinite,
                Invariant,
                Coordinating,
                Subordinating,
                Correlative,
                Fixed,
            )

        private val BY_ID: Map<String, GrammarForm> = knownEntries.associateBy { it.id }

        public fun fromId(id: String): GrammarForm = BY_ID[id] ?: Unknown(id)
    }
}

/**
 * Grammar categories. The category → allowed-forms invariant is runtime
 * ([TaxonomyInvariants.allowedFormsByCategory]) so new forms from the backend
 * cannot silently drop.
 */
public sealed interface GrammarCategory {
    public val id: String

    public data object Number : GrammarCategory {
        override val id: String = "number"
    }

    public data object Case : GrammarCategory {
        override val id: String = "case"
    }

    public data object Countability : GrammarCategory {
        override val id: String = "countability"
    }

    public data object Verb : GrammarCategory {
        override val id: String = "verb"
    }

    public data object VerbIrregular : GrammarCategory {
        override val id: String = "verb_irregular"
    }

    public data object Transitivity : GrammarCategory {
        override val id: String = "transitivity"
    }

    public data object Separability : GrammarCategory {
        override val id: String = "separability"
    }

    public data object Degree : GrammarCategory {
        override val id: String = "degree"
    }

    public data object PronounType : GrammarCategory {
        override val id: String = "pronoun_type"
    }

    public data object DeterminerType : GrammarCategory {
        override val id: String = "determiner_type"
    }

    public data object NumeralType : GrammarCategory {
        override val id: String = "numeral_type"
    }

    public data object Definiteness : GrammarCategory {
        override val id: String = "definiteness"
    }

    public data object Invariance : GrammarCategory {
        override val id: String = "invariance"
    }

    public data object ConjunctionType : GrammarCategory {
        override val id: String = "conjunction_type"
    }

    public data object ExpressionType : GrammarCategory {
        override val id: String = "expression_type"
    }

    public data class Unknown(
        override val id: String,
    ) : GrammarCategory

    public companion object {
        public val knownEntries: List<GrammarCategory> =
            listOf(
                Number,
                Case,
                Countability,
                Verb,
                VerbIrregular,
                Transitivity,
                Separability,
                Degree,
                PronounType,
                DeterminerType,
                NumeralType,
                Definiteness,
                Invariance,
                ConjunctionType,
                ExpressionType,
            )

        private val BY_ID: Map<String, GrammarCategory> = knownEntries.associateBy { it.id }

        public fun fromId(id: String): GrammarCategory = BY_ID[id] ?: Unknown(id)
    }
}

/**
 * A single grammatical feature on a sense/card. The category → form pair is
 * runtime-validated by [resolve] at the AI boundary; direct construction
 * carries [GrammarCategory.Unknown] / [GrammarForm.Unknown] through unchecked.
 */
public data class GrammarTag(
    val category: GrammarCategory,
    val form: GrammarForm,
) {
    public companion object {
        private val verbFormIds: Set<String> =
            setOf(
                GrammarForm.Infinitive.id,
                GrammarForm.PastTense.id,
                GrammarForm.PastParticiple.id,
                GrammarForm.PresentParticiple.id,
                GrammarForm.Gerund.id,
            )

        /**
         * Built-in category/form invariant for code-owned fixtures and stored
         * catalog data. Runtime taxonomy can extend this map at AI boundaries.
         */
        public val knownAllowedFormsByCategory: Map<String, Set<String>> =
            mapOf(
                GrammarCategory.Number.id to
                    setOf(GrammarForm.Singular.id, GrammarForm.Plural.id),
                GrammarCategory.Case.id to setOf(GrammarForm.NounPossessiveCase.id),
                GrammarCategory.Countability.id to
                    setOf(GrammarForm.Countable.id, GrammarForm.Uncountable.id),
                GrammarCategory.Verb.id to verbFormIds,
                GrammarCategory.VerbIrregular.id to verbFormIds,
                GrammarCategory.Transitivity.id to
                    setOf(GrammarForm.Transitive.id, GrammarForm.Intransitive.id),
                GrammarCategory.Separability.id to
                    setOf(GrammarForm.Separable.id, GrammarForm.Inseparable.id),
                GrammarCategory.Degree.id to
                    setOf(
                        GrammarForm.Positive.id,
                        GrammarForm.Comparative.id,
                        GrammarForm.Superlative.id,
                    ),
                GrammarCategory.PronounType.id to
                    setOf(
                        GrammarForm.Subjective.id,
                        GrammarForm.Objective.id,
                        GrammarForm.PronounPossessive.id,
                        GrammarForm.Reflexive.id,
                    ),
                GrammarCategory.DeterminerType.id to
                    setOf(
                        GrammarForm.Demonstrative.id,
                        GrammarForm.Quantifier.id,
                        GrammarForm.PossessiveDeterminer.id,
                        GrammarForm.Distributive.id,
                        GrammarForm.Interrogative.id,
                    ),
                GrammarCategory.NumeralType.id to
                    setOf(GrammarForm.Cardinal.id, GrammarForm.Ordinal.id),
                GrammarCategory.Definiteness.id to
                    setOf(GrammarForm.Definite.id, GrammarForm.Indefinite.id),
                GrammarCategory.Invariance.id to setOf(GrammarForm.Invariant.id),
                GrammarCategory.ConjunctionType.id to
                    setOf(
                        GrammarForm.Coordinating.id,
                        GrammarForm.Subordinating.id,
                        GrammarForm.Correlative.id,
                    ),
                GrammarCategory.ExpressionType.id to setOf(GrammarForm.Fixed.id),
            )

        /**
         * Boundary resolver. `null` [allowedFormsByCategory] = pass-through; a
         * non-null map drops off-schema pairs (the AI cannot inject pairs
         * outside the live taxonomy).
         */
        public fun resolve(
            categoryId: String,
            formId: String,
            allowedFormsByCategory: Map<String, Set<String>>? = null,
        ): GrammarTag? {
            if (allowedFormsByCategory != null) {
                val allowed = allowedFormsByCategory[categoryId] ?: return null
                if (formId !in allowed) return null
            }
            return GrammarTag(GrammarCategory.fromId(categoryId), GrammarForm.fromId(formId))
        }
    }
}
