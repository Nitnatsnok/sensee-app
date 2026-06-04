package app.sensee.srs.engine.storage

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.model.SrsCardSnapshot
import kotlin.time.Instant

public interface SrsCardStore {
    public suspend fun getCard(cardId: SrsCardId): SrsCardSnapshot?

    /**
     * Bulk variant of [getCard]. Absent ids are omitted from the result. The
     * default loops [getCard]; a persistent store overrides it with one query.
     */
    public suspend fun getCards(cardIds: Collection<SrsCardId>): Map<SrsCardId, SrsCardSnapshot> =
        cardIds.distinct().mapNotNull { id -> getCard(id)?.let { id to it } }.toMap()

    public suspend fun saveCard(card: SrsCardSnapshot)

    /**
     * Materializes [card] only when the store does not already contain that id,
     * returning the stored snapshot. Persistent stores should override this
     * with an atomic insert-if-absent so a first review cannot be clobbered by
     * a stale materialization read.
     */
    public suspend fun saveCardIfAbsent(card: SrsCardSnapshot): SrsCardSnapshot {
        val stored = getCard(card.id)
        if (stored != null) return stored
        saveCard(card)
        return getCard(card.id) ?: card
    }

    public suspend fun getDueCards(
        now: Instant,
        limit: Int,
    ): List<SrsCardSnapshot>

    /**
     * Clones the snapshot at [from] onto [to] (a different id), so review progress
     * can carry to a new identity — e.g. claiming a detached Personal copy of a
     * Service sense. A no-op when [from] has no stored snapshot; the source row is
     * left untouched (the copy starts from a snapshot, not shared state).
     */
    public suspend fun copySnapshot(
        from: SrsCardId,
        to: SrsCardId,
    ) {
        val source = getCard(from) ?: return
        saveCard(source.copy(id = to))
    }
}
