package app.sensee.feature.library.domain

/**
 * A user-facing collection of [Card]s. The same card may belong to multiple decks
 * (many-to-many).
 */
public data class Deck(
    val id: DeckId,
    val title: String,
    val description: String,
    val cardCount: Int,
    val origin: CatalogOrigin,
)

/**
 * A [Deck] loaded together with its cards. Order of [cards] reflects the order the deck was
 * authored in.
 */
public data class DeckWithCards(
    val deck: Deck,
    val cards: List<Card>,
)
