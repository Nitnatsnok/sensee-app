package app.sensee.grammar.domain

/**
 * What complement a sense takes — its valency pattern (ADR-001, canon in
 * `docs/domain/pos-and-forms.adoc`). A distinct structured axis from grammar tags
 * and preposition government: e.g. `enjoy` → {Noun, Gerund}, `want` →
 * {Noun, ToInfinitive}, `make sb do` → {BareInfinitive}.
 *
 * Sealed so a backend value the client does not know yet resolves to
 * [Unknown] (raw id) instead of being dropped or crashing exhaustive `when`s.
 */
public sealed interface ComplementType {
    public val id: String

    public data object Noun : ComplementType {
        override val id: String = "noun"
    }

    public data object Gerund : ComplementType {
        override val id: String = "gerund"
    }

    public data object ToInfinitive : ComplementType {
        override val id: String = "to_infinitive"
    }

    public data object BareInfinitive : ComplementType {
        override val id: String = "bare_infinitive"
    }

    public data object ThatClause : ComplementType {
        override val id: String = "that_clause"
    }

    public data object WhClause : ComplementType {
        override val id: String = "wh_clause"
    }

    public data object Adjective : ComplementType {
        override val id: String = "adjective"
    }

    public data object PrepositionalPhrase : ComplementType {
        override val id: String = "prepositional_phrase"
    }

    public data object Intransitive : ComplementType {
        override val id: String = "intransitive"
    }

    /** A complement-type id the client does not know yet; surfaces as its raw [id]. */
    public data class Unknown(
        override val id: String,
    ) : ComplementType

    public companion object {
        public val knownEntries: List<ComplementType> =
            listOf(
                Noun,
                Gerund,
                ToInfinitive,
                BareInfinitive,
                ThatClause,
                WhClause,
                Adjective,
                PrepositionalPhrase,
                Intransitive,
            )

        private val BY_NORMALIZED_ID: Map<String, ComplementType> =
            knownEntries.associateBy { it.id.normalizedTaxonomyId() }

        /** Case/separator-insensitive; an unknown id becomes [Unknown]. */
        public fun fromId(id: String): ComplementType = BY_NORMALIZED_ID[id.normalizedTaxonomyId()] ?: Unknown(id)
    }
}
