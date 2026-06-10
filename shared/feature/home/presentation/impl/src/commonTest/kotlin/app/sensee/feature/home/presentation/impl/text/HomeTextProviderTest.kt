package app.sensee.feature.home.presentation.impl.text

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeTextProviderTest {
    @Test
    fun `the daily goal uses the singular form for counts ending in one`() {
        assertEquals("Цель: 1 карточка в день", DefaultHomeTextProvider.quantity(HomeTextKeys.ReviewGoal, 1))
        assertEquals("Цель: 21 карточка в день", DefaultHomeTextProvider.quantity(HomeTextKeys.ReviewGoal, 21))
    }

    @Test
    fun `the daily goal uses the few form for counts ending in two to four`() {
        assertEquals("Цель: 3 карточки в день", DefaultHomeTextProvider.quantity(HomeTextKeys.ReviewGoal, 3))
        assertEquals("Цель: 22 карточки в день", DefaultHomeTextProvider.quantity(HomeTextKeys.ReviewGoal, 22))
    }

    @Test
    fun `the daily goal uses the many form for the default goal`() {
        assertEquals("Цель: 20 карточек в день", DefaultHomeTextProvider.quantity(HomeTextKeys.ReviewGoal, 20))
    }
}
