package app.sensee.feature.library.domain

import kotlinx.coroutines.flow.Flow

/**
 * Library-owned mutation of catalog ownership. Adopting a Service set turns it
 * into the user's own (Personal) material, which is what Practice plays. Practice
 * never depends on this contract — it is a write seam internal to Library.
 *
 * Reactivity: [observeAdoptedDecks] / [observeSuggestedDecks] are streams; after [adopt]
 * or [unAdopt] the Library UI updates without an explicit reload.
 */
public interface CatalogAdoptionRepository {
    /** Adopt the whole deck. Cards already owned via another deck are not duplicated. */
    public suspend fun adopt(deckId: DeckId)

    /** Drop adoption of a previously adopted deck. The derived captured deck cannot be un-adopted. */
    public suspend fun unAdopt(deckId: DeckId)

    /** Decks the user owns through adoption (excludes the derived captured deck) — the detachable subset. */
    public fun observeAdoptedDecks(): Flow<List<Deck>>

    /** Service catalog decks the user can still adopt (not yet adopted). */
    public fun observeSuggestedDecks(): Flow<List<Deck>>
}
