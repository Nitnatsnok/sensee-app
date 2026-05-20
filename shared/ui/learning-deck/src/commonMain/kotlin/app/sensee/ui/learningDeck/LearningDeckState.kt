package app.sensee.ui.learningDeck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.collections.immutable.PersistentList

/**
 * State holder for [LearningSwipeDeck].
 *
 * The state tracks the current visible item, pending programmatic swipes, and local undo history.
 * It is intended to be created with [rememberLearningDeckState] and reused across recompositions.
 */
@Stable
public class LearningDeckState<K : Any> internal constructor(
    initialIndex: Int,
) {
    public var currentIndex: Int by mutableIntStateOf(initialIndex.coerceAtLeast(0))
        private set

    internal var offset by mutableStateOf(Offset.Zero)

    internal var swipeRequest by mutableStateOf<LearningSwipeDirection?>(null)
        private set

    /**
     * The deck keys its swipe effect on this so a programmatic swipe is driven by a stable token
     * instead of [currentIndex]/items identity, which both change *during* the swipe.
     */
    internal var swipeRequestId by mutableIntStateOf(0)
        private set

    internal var isAnimating by mutableStateOf(false)

    private var currentItemKey by mutableStateOf<K?>(null)
    private val animatable = Animatable(Offset.Zero, Offset.VectorConverter)

    // Serializes every offset animation/cancel so concurrent gesture and programmatic
    // launches on the deck's UI scope cannot drive the single `animatable` at once.
    private val offsetMutationMutex = MutatorMutex()
    private val history = mutableStateListOf<LearningSwipeRecord<K>>()

    // Not snapshot-backed on purpose. Invariant: every mutation here is paired, in the same
    // call, with a snapshot-state write (currentIndex/currentItemKey/offset). Composition reads
    // it only via resolvePosition, which always also reads those snapshot fields, so the pairing
    // is what makes a mutation observable. Do not mutate it without a paired snapshot write.
    private val swipedItemKeys = mutableSetOf<K>()

    public val canUndo: Boolean
        get() = history.isNotEmpty()

    public val lastSwipeRecord: LearningSwipeRecord<K>?
        get() = history.lastOrNull()

    /**
     * Resets the deck to [index] and clears local swipe history.
     *
     * Any pending swipe request or drag offset is discarded.
     */
    public fun snapToIndex(index: Int) {
        currentIndex = index.coerceAtLeast(0)
        currentItemKey = null
        offset = Offset.Zero
        swipeRequest = null
        isAnimating = false
        swipedItemKeys.clear()
        history.clear()
    }

    /**
     * Requests a programmatic swipe for the current top card.
     *
     * The request is ignored while the deck is already animating.
     */
    public fun requestSwipe(direction: LearningSwipeDirection) {
        if (!isAnimating) {
            swipeRequest = direction
            swipeRequestId += 1
        }
    }

    /**
     * Reverts the most recent local swipe.
     *
     * The returned [LearningSwipeRecord] can be used to rollback analytics, persistence, or other
     * side effects performed by the caller.
     */
    public fun undo(): LearningSwipeRecord<K>? {
        if (isAnimating || history.isEmpty()) return null

        val record = history.removeAt(history.lastIndex)
        swipedItemKeys.remove(record.itemKey)
        currentIndex = record.itemIndex
        currentItemKey = record.itemKey
        offset = Offset.Zero
        swipeRequest = null
        isAnimating = false
        return record
    }

    internal fun consumeSwipeRequest(): LearningSwipeDirection? {
        val request = swipeRequest
        swipeRequest = null
        return request
    }

    internal fun recordSwipe(
        itemKey: K,
        itemIndex: Int,
        direction: LearningSwipeDirection,
        nextItemKey: K?,
    ) {
        history +=
            LearningSwipeRecord(
                itemKey = itemKey,
                itemIndex = itemIndex,
                direction = direction,
            )
        swipedItemKeys += itemKey
        currentIndex = itemIndex + 1
        currentItemKey = nextItemKey
        offset = Offset.Zero
        swipeRequest = null
    }

    internal fun <T> resolvePosition(
        items: PersistentList<T>,
        itemKey: (T) -> K,
    ): ResolvedDeckPosition<K> {
        if (items.isEmpty()) {
            return ResolvedDeckPosition(
                index = 0,
                itemKey = null,
            )
        }

        resolveAnchoredPosition(
            items = items,
            itemKey = itemKey,
            anchoredItemKey = currentItemKey,
            excludedItemKeys = swipedItemKeys,
        )?.let { position ->
            return position
        }

        val startIndex =
            currentIndex.coerceIn(
                minimumValue = 0,
                maximumValue = items.lastIndex,
            )

        return resolveAvailablePosition(
            items = items,
            itemKey = itemKey,
            startIndex = startIndex,
            excludedItemKeys = swipedItemKeys,
        ) ?: ResolvedDeckPosition(
            index = items.size,
            itemKey = null,
        )
    }

    internal fun syncResolvedPosition(position: ResolvedDeckPosition<K>) {
        if (currentIndex != position.index) {
            currentIndex = position.index
        }
        if (currentItemKey != position.itemKey) {
            currentItemKey = position.itemKey
        }
    }

    internal suspend fun cancelOffsetAnimation() {
        // UserInput priority preempts any in-flight animateBack/animateOut mutation so a new
        // drag deterministically wins over a settling animation.
        offsetMutationMutex.mutate(MutatePriority.UserInput) {
            animatable.snapTo(offset)
        }
        isAnimating = false
    }

    internal suspend fun animateBack(animationSpec: AnimationSpec<Offset>) {
        animateOffsetTo(
            target = Offset.Zero,
            animationSpec = animationSpec,
        )
    }

    internal suspend fun animateOut(
        target: Offset,
        animationSpec: AnimationSpec<Offset>,
    ) {
        animateOffsetTo(
            target = target,
            animationSpec = animationSpec,
        )
    }

    private suspend fun animateOffsetTo(
        target: Offset,
        animationSpec: AnimationSpec<Offset>,
    ) {
        offsetMutationMutex.mutate {
            animatable.snapTo(offset)
            animatable.animateTo(
                targetValue = target,
                animationSpec = animationSpec,
            ) {
                offset = value
            }
        }
    }
}

private inline fun <T, K : Any> resolveAnchoredPosition(
    items: PersistentList<T>,
    itemKey: (T) -> K,
    anchoredItemKey: K?,
    excludedItemKeys: Set<K>,
): ResolvedDeckPosition<K>? {
    if (anchoredItemKey == null || anchoredItemKey in excludedItemKeys) {
        return null
    }

    val anchoredIndex =
        items.indexOfFirst { item ->
            itemKey(item) == anchoredItemKey
        }
    if (anchoredIndex < 0) {
        return null
    }

    return ResolvedDeckPosition(
        index = anchoredIndex,
        itemKey = anchoredItemKey,
    )
}

private inline fun <T, K : Any> resolveAvailablePosition(
    items: PersistentList<T>,
    itemKey: (T) -> K,
    startIndex: Int,
    excludedItemKeys: Set<K>,
): ResolvedDeckPosition<K>? {
    val forwardIndex =
        findFirstAvailableIndex(
            items = items,
            itemKey = itemKey,
            excludedItemKeys = excludedItemKeys,
            startIndex = startIndex,
            endExclusive = items.size,
        )
    if (forwardIndex >= 0) {
        return ResolvedDeckPosition(
            index = forwardIndex,
            itemKey = itemKey(items[forwardIndex]),
        )
    }

    val wrappedIndex =
        findFirstAvailableIndex(
            items = items,
            itemKey = itemKey,
            excludedItemKeys = excludedItemKeys,
            startIndex = 0,
            endExclusive = startIndex,
        )
    if (wrappedIndex >= 0) {
        return ResolvedDeckPosition(
            index = wrappedIndex,
            itemKey = itemKey(items[wrappedIndex]),
        )
    }

    return null
}

private inline fun <T, K : Any> findFirstAvailableIndex(
    items: PersistentList<T>,
    itemKey: (T) -> K,
    excludedItemKeys: Set<K>,
    startIndex: Int,
    endExclusive: Int,
): Int {
    for (index in startIndex until endExclusive) {
        val key = itemKey(items[index])
        if (key !in excludedItemKeys) {
            return index
        }
    }
    return -1
}

internal data class ResolvedDeckPosition<K : Any>(
    val index: Int,
    val itemKey: K?,
)

/**
 * Creates and remembers a [LearningDeckState] for the current composition.
 *
 * @param initialIndex index of the item that should be shown first.
 */
@Composable
public fun <K : Any> rememberLearningDeckState(initialIndex: Int = 0): LearningDeckState<K> =
    remember(initialIndex) {
        LearningDeckState(initialIndex)
    }
