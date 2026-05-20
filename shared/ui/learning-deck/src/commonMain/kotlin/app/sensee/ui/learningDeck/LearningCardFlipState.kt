package app.sensee.ui.learningDeck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Mutable state object that controls which side of a [FlippableLearningCard] is currently visible.
 *
 * The state can be owned by a composable via [rememberLearningCardFlipState] or stored by a
 * higher-level state holder such as a screen model or view model.
 */
@Stable
public class LearningCardFlipState internal constructor(
    initiallyBackVisible: Boolean,
) {
    public var isBackVisible: Boolean by mutableStateOf(initiallyBackVisible)
        private set

    public fun flip() {
        isBackVisible = !isBackVisible
    }

    public fun showFront() {
        isBackVisible = false
    }

    public fun showBack() {
        isBackVisible = true
    }

    /**
     * Sets the visible side directly. The card still animates the transition through
     * [FlippableLearningCard]'s flip spec — this only sets the target side, it is not an
     * instant, animation-skipping jump.
     */
    public fun setSide(backVisible: Boolean) {
        isBackVisible = backVisible
    }
}

/**
 * Creates and remembers a [LearningCardFlipState] for the current composition.
 *
 * @param initiallyBackVisible initial visible side for the remembered state.
 */
@Composable
public fun rememberLearningCardFlipState(initiallyBackVisible: Boolean = false): LearningCardFlipState =
    remember(initiallyBackVisible) {
        LearningCardFlipState(initiallyBackVisible)
    }
