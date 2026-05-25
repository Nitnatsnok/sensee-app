package app.sensee.grammar.data

import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarLabelsLoadResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CachingGrammarLabelsProviderTest {
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
            val provider = CachingGrammarLabelsProvider(CachingGrammarTaxonomyProvider(source))

            val first = provider.labels()
            val second = provider.labels()

            assertEquals(1, source.calls, "a successful load is cached")
            assertSame(first, second, "every caller gets the same resolver instance")
        }

    @Test
    fun `a failed load returns EMPTY and is not cached so a later call retries`() =
        runTest {
            val source = FakeSource(failTimes = 1)
            val provider = CachingGrammarLabelsProvider(CachingGrammarTaxonomyProvider(source))

            val degraded = provider.labels()
            assertSame(GrammarLabels.EMPTY, degraded, "failure degrades to EMPTY, never throws")

            val recovered = provider.labels()
            assertEquals(2, source.calls, "EMPTY is not cached; the next call retries")
            assertNotSame(GrammarLabels.EMPTY, recovered, "the retry actually loaded the taxonomy")

            provider.labels()
            assertEquals(2, source.calls, "the recovered load is now cached")
        }

    @Test
    fun `awaitLabels surfaces Failed with the original cause when the load throws`() =
        runTest {
            val source = FakeSource(failTimes = 1)
            val provider = CachingGrammarLabelsProvider(CachingGrammarTaxonomyProvider(source))

            val degraded = provider.awaitLabels()
            assertIs<GrammarLabelsLoadResult.Failed>(degraded)
            assertTrue(
                degraded.cause?.message?.contains("backend down") == true,
                "cause is preserved, not swallowed to null",
            )

            val recovered = provider.awaitLabels()
            assertIs<GrammarLabelsLoadResult.Loaded>(recovered)
        }

    @Test
    fun `cachedLabels returns EMPTY before any load and the cached value after`() =
        runTest {
            val source = FakeSource()
            val provider = CachingGrammarLabelsProvider(CachingGrammarTaxonomyProvider(source))

            assertSame(GrammarLabels.EMPTY, provider.cachedLabels(), "synchronous read before load is EMPTY")
            assertEquals(0, source.calls, "the snapshot read never triggers a fetch")

            val loaded = provider.labels()
            assertSame(loaded, provider.cachedLabels(), "the snapshot now reflects the cached value")
        }

    @Test
    fun `labels and invariants projections share one taxonomy fetch`() =
        runTest {
            val source = FakeSource()
            val taxonomyProvider = CachingGrammarTaxonomyProvider(source)
            val labelsProvider = CachingGrammarLabelsProvider(taxonomyProvider)
            val invariantsProvider = CachingTaxonomyInvariantsProvider(taxonomyProvider)

            labelsProvider.labels()
            invariantsProvider.invariants()

            assertEquals(1, source.calls, "both projections read the same cached taxonomy")
        }
}
