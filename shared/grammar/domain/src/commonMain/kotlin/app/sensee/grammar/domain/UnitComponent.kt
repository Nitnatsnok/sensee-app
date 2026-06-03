package app.sensee.grammar.domain

/**
 * Structural breakdown of a multi-word lexical unit (ADR-001): the ordered
 * components of a phrasal verb, idiom, or fixed phrase plus their syntactic
 * role. Lets the family graph derive a head lemma without re-tokenising the
 * surface form, and lets practice highlight parts independently (`come` head
 * vs `across` particle in `come across`).
 *
 * A single-word unit either carries no components, or one [UnitComponent]
 * with [ComponentRole.Head]; both are equivalent and the family resolver
 * treats them as such.
 */
public data class UnitComponent(
    val text: String,
    val role: ComponentRole,
    val salience: ComponentSalience? = null,
)

/**
 * Significance of a [UnitComponent] to the unit's meaning, for UI emphasis:
 * `come` is [Primary] and `across` [Secondary] in `come across`, while `the` is
 * [Incidental] in `kick the bucket`. Optional ([UnitComponent.salience] is
 * `null` when the source did not rank it). Sealed so a backend-only level
 * resolves to [Unknown] rather than being dropped (ADR-006), matching
 * [ComponentRole].
 */
public sealed interface ComponentSalience {
    public val id: String

    /** The semantic head — the part that carries the unit's core meaning. */
    public data object Primary : ComponentSalience {
        override val id: String = "primary"
    }

    /** A meaning-shaping particle, preposition, or fixed object. */
    public data object Secondary : ComponentSalience {
        override val id: String = "secondary"
    }

    /** A fixed grammatical filler that carries no independent weight (`the`, `a`). */
    public data object Incidental : ComponentSalience {
        override val id: String = "incidental"
    }

    /** A salience id the client does not know yet; surfaces as its raw [id]. */
    public data class Unknown(
        override val id: String,
    ) : ComponentSalience

    public companion object {
        public val knownEntries: List<ComponentSalience> = listOf(Primary, Secondary, Incidental)

        private val BY_NORMALIZED_ID: Map<String, ComponentSalience> =
            knownEntries.associateBy { it.id.normalizedTaxonomyId() }

        /** Case/separator-insensitive; an unknown id becomes [Unknown]. */
        public fun fromId(id: String): ComponentSalience = BY_NORMALIZED_ID[id.normalizedTaxonomyId()] ?: Unknown(id)
    }
}

/**
 * Syntactic role of a [UnitComponent] inside its parent unit (canon
 * `docs/domain/pos-and-forms.adoc`). Sealed so a backend-only role resolves to
 * [Unknown] (raw id) instead of being dropped or crashing exhaustive `when`s,
 * matching the rest of the grammar taxonomy (ADR-006).
 */
public sealed interface ComponentRole {
    public val id: String

    /** The lexical head: verb in a phrasal verb, governing noun in a noun phrase. */
    public data object Head : ComponentRole {
        override val id: String = "head"
    }

    /** A directional/aspectual particle: `up`, `down`, `out`, `across`. */
    public data object Particle : ComponentRole {
        override val id: String = "particle"
    }

    /** A governed preposition: `with` in `come up with`, `on` in `rely on`. */
    public data object Preposition : ComponentRole {
        override val id: String = "preposition"
    }

    /** A frozen lexical object inside an idiom: `the bucket` in `kick the bucket`. */
    public data object FixedObject : ComponentRole {
        override val id: String = "fixed_object"
    }

    /** An adjectival or adverbial modifier inside a fixed phrase. */
    public data object Modifier : ComponentRole {
        override val id: String = "modifier"
    }

    /** A residual role that does not fit any of the named ones; carried as evidence, not error. */
    public data object Other : ComponentRole {
        override val id: String = "other"
    }

    /** A role id the client does not know yet; surfaces as its raw [id]. */
    public data class Unknown(
        override val id: String,
    ) : ComponentRole

    public companion object {
        public val knownEntries: List<ComponentRole> =
            listOf(Head, Particle, Preposition, FixedObject, Modifier, Other)

        private val BY_NORMALIZED_ID: Map<String, ComponentRole> =
            knownEntries.associateBy { it.id.normalizedTaxonomyId() }

        /** Case/separator-insensitive; an unknown id becomes [Unknown]. */
        public fun fromId(id: String): ComponentRole = BY_NORMALIZED_ID[id.normalizedTaxonomyId()] ?: Unknown(id)
    }
}
