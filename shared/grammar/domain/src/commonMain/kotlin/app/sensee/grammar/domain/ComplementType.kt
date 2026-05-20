package app.sensee.grammar.domain

/**
 * What complement a sense takes — its valency pattern (ADR-001, canon in
 * `docs/pos-and-forms.adoc`). A distinct structured axis from grammar tags
 * and preposition government: e.g. `enjoy` → {Noun, Gerund}, `want` →
 * {Noun, ToInfinitive}, `make sb do` → {BareInfinitive}. A sense carries a
 * list (≥0); surfaced as a grammar feature on the card so the learner sees
 * "takes -ing / takes to-infinitive".
 */
public enum class ComplementType(
    public val id: String,
) {
    Noun("noun"),
    Gerund("gerund"),
    ToInfinitive("to_infinitive"),
    BareInfinitive("bare_infinitive"),
    ThatClause("that_clause"),
    WhClause("wh_clause"),
    Adjective("adjective"),
    PrepositionalPhrase("prepositional_phrase"),
    Intransitive("intransitive"),
    ;

    public companion object {
        /** Resolves a wire id, ignoring separators/case; null when unknown (dropped at the boundary). */
        public fun fromId(id: String): ComplementType? {
            val normalized = id.normalizedGrammarId()
            return entries.firstOrNull { it.id.normalizedGrammarId() == normalized }
        }
    }
}
