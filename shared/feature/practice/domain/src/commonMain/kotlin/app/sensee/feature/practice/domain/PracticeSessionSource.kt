package app.sensee.feature.practice.domain

/**
 * What a practice session draws its cards from. The session machinery (queue
 * projection, in-session reinjection, FSRS scheduling) is identical regardless of
 * source; only how the initial card list is obtained differs. [Deck] runs a ready
 * catalog deck; [Due] runs the cards that are due now (Home's «Стоит повторить»).
 *
 * EB-3 widens this from deck-only toward arbitrary subsets (Library selections,
 * mixed decks, manual sets); those are not modelled yet.
 */
public sealed interface PracticeSessionSource {
    public data class Deck(
        val deckId: String,
    ) : PracticeSessionSource

    public data object Due : PracticeSessionSource
}

/** A stable, distinct string per source — used as a logic/session key. */
public val PracticeSessionSource.key: String
    get() =
        when (this) {
            is PracticeSessionSource.Deck -> "deck:$deckId"
            PracticeSessionSource.Due -> "due"
        }
