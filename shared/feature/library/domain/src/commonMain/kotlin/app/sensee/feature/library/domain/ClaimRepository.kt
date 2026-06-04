package app.sensee.feature.library.domain

/**
 * Take a detached Personal copy of a Service sense ("claim it"): a fresh Personal
 * sense_id, a content snapshot, and the sense's SRS cloned onto the new id, all in
 * one transaction. The copy is independent — a later upstream re-sync of the
 * subscribed deck never touches it (the mirror-vs-own split, ADR-001), and the
 * captured deck (Personal Confirmed senses) surfaces it. Editing a subscribed
 * sense is "claim, then edit" (EB-12).
 */
public interface ClaimRepository {
    /** Claim the sense behind [cardId] (a Service card); returns the new Personal card id. */
    public suspend fun claim(cardId: CardId): CardId
}
