package app.sensee.lexicon.domain

/**
 * A persisted sense as the store hands it back: identity and row metadata live
 * here, while [sense] stays pure lexical content. Library and Practice project
 * from this read model; the editor edits [sense] and writes it back through
 * [SenseWriteRepository].
 *
 * [lemmaKey] is the grouping key (head-lemma family), carried on the row so
 * projections group without re-deriving. CEFR and part of speech are read from
 * [sense] — they are content, not row metadata, so they are not duplicated here.
 */
public data class StoredSense(
    val id: SenseId,
    val status: SenseStatus,
    val origin: SenseOrigin,
    val sourceRef: String?,
    val lemmaKey: String,
    val updatedAtEpochMs: Long,
    val sense: Sense,
)

/**
 * Binary lifecycle of a stored sense. A [Draft] is still being authored (it may
 * carry an empty translation and no example, and has no SRS state); a [Confirmed]
 * sense has passed the confirm-gate and is practiced. No intermediate states.
 */
public enum class SenseStatus {
    Draft,
    Confirmed,
}

/**
 * Who owns a stored sense. [Personal] senses are the user's own material
 * (captured, manual, or claimed from a service set). [Service] senses mirror
 * subscribed catalog material, keyed by `source_ref`; a re-sync overwrites their
 * content but never a [Personal] row.
 */
public enum class SenseOrigin {
    Personal,
    Service,
}
