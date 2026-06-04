package app.sensee.lexicon.domain

/**
 * How a write resolves identity, so one sense keeps one [SenseId] (and its SRS)
 * by default while still allowing explicit "create new" and "replace this row".
 */
public sealed interface WriteIntent {
    /**
     * Reuse an existing [SenseId] on an exact match — `content_key` among
     * `Personal`, `source_ref` among `Service` — and mint only on a miss. The
     * default: same sense in, same id out, SRS preserved.
     */
    public data object ResolveOrMint : WriteIntent

    /**
     * Replace the content of a specific row in place, keeping its [SenseId] and
     * SRS. Used for edits and for the duplicate-dialog "replace existing".
     */
    public data class UpdateExisting(
        val id: SenseId,
    ) : WriteIntent

    /**
     * Always mint a new row, even when the `content_key` matches an existing
     * sense. Used for the duplicate-dialog "create new" and for `claim` (a
     * detached Personal copy that must not adopt another row's SRS).
     */
    public data object ForceMint : WriteIntent
}
