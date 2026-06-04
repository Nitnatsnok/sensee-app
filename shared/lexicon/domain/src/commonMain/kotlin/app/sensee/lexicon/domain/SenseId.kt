package app.sensee.lexicon.domain

import kotlin.jvm.JvmInline

/**
 * Stable identity of a stored sense — the canonical primary key and the anchor
 * for SRS state, symmetric to `SrsCardId`. It survives any content edit: editing
 * a translation or re-deriving the content key never changes the [value].
 *
 * Personal senses mint a random UUID; Service senses derive a deterministic,
 * namespaced id from their `source_ref` (stable across re-sync). The [value]
 * never contains `:` so the SRS form-key `${value}:form:${formId}` parses
 * unambiguously.
 */
@JvmInline
public value class SenseId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "SenseId must not be blank" }
        require(!value.contains(':')) { "SenseId must not contain ':' (it breaks the SRS form-key)" }
    }

    override fun toString(): String = value
}
