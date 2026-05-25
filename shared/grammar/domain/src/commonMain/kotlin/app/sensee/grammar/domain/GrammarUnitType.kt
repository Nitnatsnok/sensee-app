package app.sensee.grammar.domain

/**
 * Lexical unit type of a sense (ADR-001/EB-1, canon `docs/pos-and-forms.adoc`).
 * Sealed so a backend-only value resolves to [Unknown] (raw id) instead of
 * being dropped or crashing exhaustive `when`s.
 */
public sealed interface GrammarUnitType {
    public val id: String

    public data object Noun : GrammarUnitType {
        override val id: String = "noun"
    }

    public data object Verb : GrammarUnitType {
        override val id: String = "verb"
    }

    public data object IrregularVerb : GrammarUnitType {
        override val id: String = "irregular_verb"
    }

    public data object PhrasalVerb : GrammarUnitType {
        override val id: String = "phrasal_verb"
    }

    public data object ModalVerb : GrammarUnitType {
        override val id: String = "modal_verb"
    }

    public data object AuxiliaryVerb : GrammarUnitType {
        override val id: String = "auxiliary_verb"
    }

    public data object Adjective : GrammarUnitType {
        override val id: String = "adjective"
    }

    public data object Adverb : GrammarUnitType {
        override val id: String = "adverb"
    }

    public data object Preposition : GrammarUnitType {
        override val id: String = "preposition"
    }

    public data object Conjunction : GrammarUnitType {
        override val id: String = "conjunction"
    }

    public data object Pronoun : GrammarUnitType {
        override val id: String = "pronoun"
    }

    public data object Determiner : GrammarUnitType {
        override val id: String = "determiner"
    }

    public data object Numeral : GrammarUnitType {
        override val id: String = "numeral"
    }

    public data object Article : GrammarUnitType {
        override val id: String = "article"
    }

    public data object Idiom : GrammarUnitType {
        override val id: String = "idiom"
    }

    public data object Phrase : GrammarUnitType {
        override val id: String = "phrase"
    }

    public data object Interjection : GrammarUnitType {
        override val id: String = "interjection"
    }

    /** A unit-type id the client does not know yet; surfaces as its raw [id]. */
    public data class Unknown(
        override val id: String,
    ) : GrammarUnitType

    public companion object {
        public val knownEntries: List<GrammarUnitType> =
            listOf(
                Noun,
                Verb,
                IrregularVerb,
                PhrasalVerb,
                ModalVerb,
                AuxiliaryVerb,
                Adjective,
                Adverb,
                Preposition,
                Conjunction,
                Pronoun,
                Determiner,
                Numeral,
                Article,
                Idiom,
                Phrase,
                Interjection,
            )

        private val BY_NORMALIZED_ID: Map<String, GrammarUnitType> =
            knownEntries.associateBy { it.id.normalizedTaxonomyId() }

        /** Case/separator-insensitive; an unknown id becomes [Unknown]. */
        public fun fromId(id: String): GrammarUnitType = BY_NORMALIZED_ID[id.normalizedTaxonomyId()] ?: Unknown(id)
    }
}
