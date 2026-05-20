package app.sensee.feature.vocabularyEditor.domain

import kotlin.jvm.JvmInline

@JvmInline
public value class EntryId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "EntryId must not be blank" }
    }

    override fun toString(): String = value
}

/**
 * A captured lexical item. Created as a [EntryStatus.Draft] from a single
 * input field (the input language is auto-detected, not supplied — ADR-001)
 * and refined toward [EntryStatus.Confirmed]; a partially filled draft is
 * valid by design (capture is frictionless).
 */
public data class LexicalEntry(
    val id: EntryId,
    val term: String,
    val status: EntryStatus,
    val meanings: List<Meaning> = emptyList(),
)

/**
 * Two states only: an entry is either still being captured ([Draft] — possibly
 * enriched, nothing confirmed yet) or [Confirmed] (has user-confirmed meanings,
 * usable as catalog/practice material). A richer triage lifecycle
 * (in-refinement / has-candidates) is deliberately deferred until the Library
 * "unfinished capture" inbox gives those states a real consumer
 * (`docs/evolution-backlog.adoc`, EB-6).
 */
public enum class EntryStatus {
    Draft,
    Confirmed,
}
