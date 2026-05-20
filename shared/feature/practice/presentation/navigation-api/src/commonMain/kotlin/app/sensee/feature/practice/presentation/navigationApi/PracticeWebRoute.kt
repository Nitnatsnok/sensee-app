package app.sensee.feature.practice.presentation.navigationApi

/**
 * Single source of truth for the Practice section's URL sub-path, used by both
 * Decompose Web Navigation (forward: config -> path/params) and cold-start deep
 * link resolution (reverse: path/params -> config). Keeping both directions here
 * keeps them from drifting apart. The path is appended after the section
 * segment, e.g. `/practice/deck/{deckId}`.
 */
public object PracticeWebRoute {
    private const val DECK = "deck"
    private const val CARD = "card"
    private const val FOCUS_PARAM = "focus"

    public fun pathFor(config: PracticeConfig): String? =
        when (config) {
            PracticeConfig.Home -> null
            is PracticeConfig.DeckPractice -> "$DECK/${config.deckId}"
            is PracticeConfig.CardDetail -> "$CARD/${config.cardId}"
        }

    public fun parametersFor(config: PracticeConfig): Map<String, String>? =
        when (config) {
            is PracticeConfig.DeckPractice ->
                config.focusedCardId?.let { mapOf(FOCUS_PARAM to it) }

            PracticeConfig.Home, is PracticeConfig.CardDetail -> null
        }

    public fun parse(
        segments: List<String>,
        parameters: Map<String, String>,
    ): PracticeConfig? =
        when {
            segments.size == 2 && segments[0] == DECK ->
                PracticeConfig.DeckPractice(
                    deckId = segments[1],
                    focusedCardId = parameters[FOCUS_PARAM],
                )

            segments.size == 2 && segments[0] == CARD ->
                PracticeConfig.CardDetail(cardId = segments[1])

            else -> null
        }
}
