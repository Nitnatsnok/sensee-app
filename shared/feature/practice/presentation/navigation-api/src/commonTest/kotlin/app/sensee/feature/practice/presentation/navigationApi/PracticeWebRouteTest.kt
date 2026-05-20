package app.sensee.feature.practice.presentation.navigationApi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins the Practice URL scheme used by both Decompose Web Navigation (forward)
 * and cold-start deep-link resolution (reverse). The round-trip must hold so a
 * shared/typed `/practice/...` URL restores the exact screen.
 */
class PracticeWebRouteTest {
    @Test
    fun `Home contributes no path segment`() {
        assertNull(PracticeWebRoute.pathFor(PracticeConfig.Home))
        assertNull(PracticeWebRoute.parametersFor(PracticeConfig.Home))
    }

    @Test
    fun `DeckPractice round-trips through path and focus param`() {
        val config = PracticeConfig.DeckPractice(deckId = "d1", focusedCardId = "c9")

        assertEquals("deck/d1", PracticeWebRoute.pathFor(config))
        assertEquals(mapOf("focus" to "c9"), PracticeWebRoute.parametersFor(config))
        assertEquals(
            config,
            PracticeWebRoute.parse(listOf("deck", "d1"), mapOf("focus" to "c9")),
        )
    }

    @Test
    fun `DeckPractice without a focused card omits the param`() {
        val config = PracticeConfig.DeckPractice(deckId = "d1")

        assertNull(PracticeWebRoute.parametersFor(config))
        assertEquals(config, PracticeWebRoute.parse(listOf("deck", "d1"), emptyMap()))
    }

    @Test
    fun `CardDetail round-trips through path`() {
        val config = PracticeConfig.CardDetail(cardId = "c2")

        assertEquals("card/c2", PracticeWebRoute.pathFor(config))
        assertEquals(config, PracticeWebRoute.parse(listOf("card", "c2"), emptyMap()))
    }

    @Test
    fun `an unknown or empty sub-path is not a Practice screen`() {
        assertNull(PracticeWebRoute.parse(emptyList(), emptyMap()))
        assertNull(PracticeWebRoute.parse(listOf("bogus"), emptyMap()))
        assertNull(PracticeWebRoute.parse(listOf("deck"), emptyMap()))
    }
}
