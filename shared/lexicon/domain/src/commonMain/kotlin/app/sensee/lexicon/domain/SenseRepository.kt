package app.sensee.lexicon.domain

import kotlinx.coroutines.flow.Flow

/**
 * Read access to the single canonical sense store. Projections (Library,
 * Practice) depend only on this — they never write — so the "one write side,
 * many read projections" boundary is structural, not a convention.
 */
public interface SenseReadRepository {
    public suspend fun getById(id: SenseId): StoredSense?

    /**
     * Bulk [getById] — missing ids are omitted and the order is unspecified, so a
     * caller that needs a specific order re-orders by its own id list. Lets a deck
     * load every member sense in one query instead of one round-trip per card.
     */
    public suspend fun getByIds(ids: Collection<SenseId>): List<StoredSense>

    public fun observe(): Flow<List<StoredSense>>

    public suspend fun listByStatus(status: SenseStatus): List<StoredSense>

    public suspend fun listByLemmaKey(lemmaKey: String): List<StoredSense>
}

/**
 * Write access to the canonical sense store, used by the editor and by service
 * ingest. [upsert] resolves identity per [intent] and is the single place the
 * confirm-gate is enforced on `Confirmed` writes.
 *
 * `claim` (a detached copy plus SRS clone plus deck membership) is intentionally
 * **not** here: it spans SRS and deck ownership and lives as an orchestrator
 * outside `lexicon`. This contract only mints the Personal content copy.
 */
public interface SenseWriteRepository {
    /**
     * Persist [sense] under [origin]/[sourceRef] according to [intent] and return
     * the stored row. Requires a non-null [sourceRef] for [SenseOrigin.Service];
     * requires the confirm-gate ([Sense.requireConfirmable]) when [status] is
     * [SenseStatus.Confirmed].
     */
    public suspend fun upsert(
        sense: Sense,
        status: SenseStatus,
        origin: SenseOrigin,
        sourceRef: String? = null,
        intent: WriteIntent = WriteIntent.ResolveOrMint,
    ): StoredSense

    /**
     * Confirm [senses] as a Personal batch. The durable store persists the batch
     * atomically — the confirm-gate ([Sense.requireConfirmable]) is checked across
     * every sense before any write, so a rejected sense cannot leave the rest
     * partially persisted. Each sense resolves to a stable sense_id as for an
     * [upsert] with [WriteIntent.ResolveOrMint].
     */
    public suspend fun confirmAll(senses: List<Sense>): List<StoredSense>
}
