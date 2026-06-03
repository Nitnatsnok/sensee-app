package app.sensee.grammar.domain

/**
 * A structured preposition-government cue on a sense (ADR-001, canon in
 * `docs/domain/pos-and-forms.adoc`): a group of prepositions that are
 * **interchangeable for this same sense** (e.g. `different` → from / to /
 * than), with an optional [example] illustrating the construction when it is
 * not already shown by the sense's contextual applications.
 *
 * Prepositions that *change the meaning* (`look at` vs `look after` vs
 * `look for`) are NOT modelled here — they are separate senses (sense
 * segregation). A sense carries a list of these groups (usually 0–1); an
 * empty list means the sense has no preposition government.
 */
public data class PrepositionGovernment(
    val alternatives: List<String>,
    val example: String? = null,
) {
    init {
        require(alternatives.isNotEmpty()) { "PrepositionGovernment needs at least one preposition" }
    }
}
