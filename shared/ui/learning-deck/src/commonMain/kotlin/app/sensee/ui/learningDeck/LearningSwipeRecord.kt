package app.sensee.ui.learningDeck

/**
 * Information about a completed swipe stored by [LearningDeckState].
 *
 * The record keeps both the stable item key and the resolved index so callers can use it for undo,
 * analytics, or persistence.
 */
public data class LearningSwipeRecord<K : Any>(
    val itemKey: K,
    val itemIndex: Int,
    val direction: LearningSwipeDirection,
)
