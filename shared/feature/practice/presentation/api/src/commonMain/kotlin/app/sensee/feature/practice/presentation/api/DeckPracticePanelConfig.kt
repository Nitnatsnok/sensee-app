package app.sensee.feature.practice.presentation.api

public sealed interface DeckPracticePanelConfig {
    public data class Deck(
        val deckId: String,
    ) : DeckPracticePanelConfig

    public data class CardDetail(
        val cardId: String,
    ) : DeckPracticePanelConfig
}
