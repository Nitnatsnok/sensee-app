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
     * Sync the Service catalog from the remote source into the local cache. Library calls this
     * on screen open / Retry; emissions from [observeOwnedMaterial] and from the adoption
     * repository's streams reflect the refreshed local state automatically.
     */
    public suspend fun refreshFromRemote()

    public suspend fun loadDeck(deckId: DeckId): DeckWithCards

    public suspend fun loadCard(cardId: CardId): Card

    public suspend fun loadLemma(lemmaId: LemmaId): Lemma
}
