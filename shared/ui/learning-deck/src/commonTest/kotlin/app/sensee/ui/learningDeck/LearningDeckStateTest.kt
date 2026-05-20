package app.sensee.ui.learningDeck

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LearningDeckStateTest {
    @Test
    fun resolve_tracks_next_card_after_swiped_item_is_removed() {
        val state = LearningDeckState<String>(initialIndex = 0)
        val initialItems =
            persistentListOf(
                TestCard("a"),
                TestCard("b"),
                TestCard("c"),
            )

        state.syncResolvedPosition(
            state.resolvePosition(initialItems) { it.id },
        )

        state.recordSwipe(
            itemKey = "a",
            itemIndex = 0,
            direction = LearningSwipeDirection.End,
            nextItemKey = "b",
        )

        val updatedItems =
            persistentListOf(
                TestCard("b"),
                TestCard("c"),
            )

        val resolved = state.resolvePosition(updatedItems) { it.id }
        state.syncResolvedPosition(resolved)

        assertEquals(0, resolved.index)
        assertEquals(0, state.currentIndex)
    }

    @Test
    fun undo_restores_swiped_card_when_it_is_still_present() {
        val state = LearningDeckState<String>(initialIndex = 0)
        val items =
            persistentListOf(
                TestCard("a"),
                TestCard("b"),
                TestCard("c"),
            )

        state.syncResolvedPosition(
            state.resolvePosition(items) { it.id },
        )

        state.recordSwipe(
            itemKey = "a",
            itemIndex = 0,
            direction = LearningSwipeDirection.End,
            nextItemKey = "b",
        )

        val record = state.undo()
        val resolved = state.resolvePosition(items) { it.id }
        state.syncResolvedPosition(resolved)

        assertNotNull(record)
        assertEquals("a", record.itemKey)
        assertEquals(0, resolved.index)
        assertEquals(0, state.currentIndex)
        assertFalse(state.canUndo)
    }

    @Test
    fun undo_falls_back_to_next_available_card_when_removed_item_is_missing() {
        val state = LearningDeckState<String>(initialIndex = 0)
        val initialItems =
            persistentListOf(
                TestCard("a"),
                TestCard("b"),
                TestCard("c"),
            )

        state.syncResolvedPosition(
            state.resolvePosition(initialItems) { it.id },
        )

        state.recordSwipe(
            itemKey = "a",
            itemIndex = 0,
            direction = LearningSwipeDirection.End,
            nextItemKey = "b",
        )
        assertTrue(state.canUndo)

        state.undo()

        val updatedItems =
            persistentListOf(
                TestCard("b"),
                TestCard("c"),
            )

        val resolved = state.resolvePosition(updatedItems) { it.id }
        state.syncResolvedPosition(resolved)

        assertEquals(0, resolved.index)
        assertEquals(0, state.currentIndex)
        assertFalse(state.canUndo)
    }

    @Test
    fun resolve_returns_empty_position_when_all_items_are_swiped() {
        val state = LearningDeckState<String>(initialIndex = 0)
        val items =
            persistentListOf(
                TestCard("a"),
                TestCard("b"),
            )

        state.recordSwipe(
            itemKey = "a",
            itemIndex = 0,
            direction = LearningSwipeDirection.End,
            nextItemKey = "b",
        )
        state.recordSwipe(
            itemKey = "b",
            itemIndex = 1,
            direction = LearningSwipeDirection.End,
            nextItemKey = null,
        )

        val resolved = state.resolvePosition(items) { it.id }

        assertEquals(items.size, resolved.index)
    }

    @Test
    fun `accepted swipe request increments the request token`() {
        val state = LearningDeckState<String>(initialIndex = 0)

        val before = state.swipeRequestId
        state.requestSwipe(LearningSwipeDirection.End)

        assertEquals(before + 1, state.swipeRequestId)
        assertEquals(LearningSwipeDirection.End, state.swipeRequest)
    }

    @Test
    fun `swipe request is ignored while animating and token is unchanged`() {
        val state = LearningDeckState<String>(initialIndex = 0)
        state.isAnimating = true

        val before = state.swipeRequestId
        state.requestSwipe(LearningSwipeDirection.End)

        assertEquals(before, state.swipeRequestId)
        assertNull(state.swipeRequest)
    }

    @Test
    fun `consuming a swipe request clears direction but keeps the token`() {
        val state = LearningDeckState<String>(initialIndex = 0)
        state.requestSwipe(LearningSwipeDirection.Up)
        val token = state.swipeRequestId

        val consumed = state.consumeSwipeRequest()

        assertEquals(LearningSwipeDirection.Up, consumed)
        assertNull(state.swipeRequest)
        assertEquals(token, state.swipeRequestId)
    }

    private data class TestCard(
        val id: String,
    )
}
