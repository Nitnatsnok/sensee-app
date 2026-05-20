package app.sensee.grammar.domain

public enum class GrammarUnitType {
    Noun,
    Verb,
    IrregularVerb,
    PhrasalVerb,
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
    ;

    public companion object {
        /** Resolves a wire/taxonomy id (e.g. `phrasal_verb`) to its enum, ignoring separators/case. */
        public fun fromId(id: String): GrammarUnitType? {
            val normalized = id.normalizedGrammarId()
            return entries.firstOrNull { it.name.normalizedGrammarId() == normalized }
        }
    }
}

internal fun String.normalizedGrammarId(): String = filter { it.isLetterOrDigit() }.lowercase()
