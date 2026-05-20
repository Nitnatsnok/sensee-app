package app.sensee.grammar.data

import app.sensee.grammar.domain.GrammarLabels
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class WarmingGrammarLabelsProviderTest {
    private class FakeSource(
        private val failTimes: Int = 0,
    ) : GrammarTaxonomySource {
        var calls = 0
            private set

        override suspend fun getGrammarTaxonomy(): GrammarTaxonomyDto {
            calls++
            if (calls <= failTimes) error("backend down")
            return GrammarTaxonomyDto()
        }
    }

    @Test
    fun `the taxonomy is fetched once and shared by every caller`() =
        runTest {
            val source = FakeSource()
            val provider = WarmingGrammarLabelsProvider(source)

            val first = provider.labels()
            val second = provider.labels()

            assertEquals(1, source.calls, "a successful load is memoized")
            assertSame(first, second, "every caller gets the same resolver instance")
        }

    @Test
    fun `a failed load returns EMPTY and is not cached so a later call retries`() =
        runTest {
            val source = FakeSource(failTimes = 1)
            val provider = WarmingGrammarLabelsProvider(source)

            val degraded = provider.labels()
            assertSame(GrammarLabels.EMPTY, degraded, "failure degrades to EMPTY, never throws")

            val recovered = provider.labels()
            assertEquals(2, source.calls, "EMPTY is not cached; the next call retries")
            assertNotSame(GrammarLabels.EMPTY, recovered, "the retry actually loaded the taxonomy")

            provider.labels()
            assertEquals(2, source.calls, "the recovered load is now memoized")
        }
}
