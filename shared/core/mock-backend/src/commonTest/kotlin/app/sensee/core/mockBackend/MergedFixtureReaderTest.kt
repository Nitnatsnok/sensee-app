package app.sensee.core.mockBackend

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MergedFixtureReaderTest {
    @Test
    fun `merges fixture paths from every set`() =
        runTest {
            val reader =
                MergedFixtureReader(
                    setOf(
                        fixtureSet("a/one" to "1"),
                        fixtureSet("b/two" to "2"),
                    ),
                )

            assertEquals("1", reader.readText("a/one"))
            assertEquals("2", reader.readText("b/two"))
            assertNull(reader.readText("c/missing"))
        }

    @Test
    fun `duplicate fixture path across sets fails fast instead of last-writer-wins`() {
        assertFailsWith<IllegalArgumentException> {
            MergedFixtureReader(
                setOf(
                    fixtureSet("shared/path" to "from-a"),
                    fixtureSet("shared/path" to "from-b"),
                ),
            )
        }
    }

    private fun fixtureSet(vararg entries: Pair<String, String>): MockFixtureSet =
        object : MockFixtureSet {
            override val fixtures: Map<String, String> = mapOf(*entries)
        }
}
