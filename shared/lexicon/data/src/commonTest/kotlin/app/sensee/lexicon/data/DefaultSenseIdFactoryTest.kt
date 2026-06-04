package app.sensee.lexicon.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DefaultSenseIdFactoryTest {
    private val factory = DefaultSenseIdFactory()

    @Test
    fun `a service id is deterministic for the same source`() {
        assertEquals(
            factory.forServiceSource("deck-1/card-3"),
            factory.forServiceSource("deck-1/card-3"),
        )
    }

    @Test
    fun `different sources get different service ids`() {
        assertNotEquals(
            factory.forServiceSource("deck-1/card-3"),
            factory.forServiceSource("deck-1/card-4"),
        )
    }

    @Test
    fun `a service id never contains a colon even when the source does`() {
        val id = factory.forServiceSource("urn:deck:1:card:3")

        assertTrue(':' !in id.value, "the SRS form-key relies on a colon-free sense_id")
    }

    @Test
    fun `personal ids are unique`() {
        assertNotEquals(factory.mintPersonal(), factory.mintPersonal())
    }
}
