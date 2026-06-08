package app.sensee.feature.library.presentation.navigationApi

import app.sensee.core.decompose.navigation.ScreenConfig
import kotlinx.serialization.Serializable

@Serializable
public sealed interface LibraryConfig : ScreenConfig {
    @Serializable
    public object Home : LibraryConfig

    /** Read-only browse of a deck's cards (owned, captured, or a Service suggestion). */
    @Serializable
    public data class DeckDetail(
        val deckId: String,
    ) : LibraryConfig
}
