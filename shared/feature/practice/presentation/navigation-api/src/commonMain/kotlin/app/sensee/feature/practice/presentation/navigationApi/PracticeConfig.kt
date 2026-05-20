package app.sensee.feature.practice.presentation.navigationApi

import app.sensee.core.decompose.navigation.ScreenConfig
import kotlinx.serialization.Serializable

@Serializable
public sealed interface PracticeConfig : ScreenConfig {
    @Serializable
    public data object Home : PracticeConfig

    @Serializable
    public data class DeckPractice(
        val deckId: String,
        val focusedCardId: String? = null,
    ) : PracticeConfig

    @Serializable
    public data class CardDetail(
        val cardId: String,
    ) : PracticeConfig
}
