package app.sensee.feature.library.domain

import kotlinx.coroutines.flow.Flow

/**
 * Read access to the learning catalog (decks, cards, lemmas). Owned by Library; Practice is a
 * read-only consumer. Mutation of learning progress lives behind a separate practice-owned
 * contract, not here.
 *
 * Reactivity: [observeOwnedMaterial] is the canonical stream of the user's own decks; UI
 * driven by it updates without manual reloads. Remote sync of the Service catalog is an
 * explicit out-of-band action ([refreshFromRemote]), not a side effect of reading.
 */
public interface CatalogRepository {
    /** Adopted decks plus the derived captured deck — what Practice plays and Library lists as owned. */
    public fun observeOwnedMaterial(): Flow<List<Deck>>

    /**
     * Sync the Service catalog from the remote source into the local cache, including details
     * for already-adopted Service decks. Library and Practice call this on screen open / Retry;
     * emissions from [observeOwnedMaterial] and from the adoption repository's streams reflect
     * the refreshed local state automatically.
     */
    public suspend fun refreshFromRemote()

    public suspend fun loadDeck(deckId: DeckId): DeckWithCards

    /**
     * Read-only projection of a deck's cards for browsing in Library, writing nothing.
     * Owned (subscribed) and captured decks are read from the local cache; a not-yet-owned
     * Service suggestion (no local membership) is projected straight from the remote source,
     * never ingested.
     *
     * Distinct from [loadDeck], a pure local read that assumes the deck's content is already
     * cached (an unsubscribed Service deck reads as empty there). Adopting a Service deck stays
     * a separate, explicit action — previewing it must not silently add it.
     */
    public suspend fun previewDeck(deckId: DeckId): DeckWithCards

    public suspend fun loadCard(cardId: CardId): Card

    /**
     * Bulk read of cards by id for an ad-hoc session (e.g. Home's due subset). The result
     * follows the input order and omits ids with no confirmed sense — the caller's order
     * (such as due-date ascending) is preserved, mirroring [app.sensee.lexicon.domain.SenseReadRepository.getByIds].
     */
    public suspend fun loadCards(cardIds: List<CardId>): List<Card>

    public suspend fun loadLemma(lemmaId: LemmaId): Lemma
}
