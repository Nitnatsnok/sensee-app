package app.sensee.ui.learningDeck

import androidx.compose.runtime.Stable

/**
 * Scope passed to [LearningSwipeDeck]'s [cardContent].
 *
 * It exposes the deck-specific metadata needed to render card overlays or visual feedback without
 * leaking the internal implementation details of the stack.
 */
@Stable
public class LearningSwipeDeckCardScope internal constructor(
    public val isTopCard: Boolean,
    public val stackIndex: Int,
    public val topCardSwipeState: LearningDeckSwipeState,
)
