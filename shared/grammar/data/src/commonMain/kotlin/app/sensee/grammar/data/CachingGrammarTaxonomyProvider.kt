package app.sensee.grammar.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@SingleIn(AppScope::class)
@Inject
public class CachingGrammarTaxonomyProvider(
    private val source: GrammarTaxonomySource,
) {
    private val mutex = Mutex()
    private var cached: GrammarTaxonomyDto? = null

    public fun cachedTaxonomy(): GrammarTaxonomyDto? = cached

    public suspend fun taxonomy(): GrammarTaxonomyLoadResult {
        cached?.let { return GrammarTaxonomyLoadResult.Loaded(it) }
        return mutex.withLock {
            cached?.let { return@withLock GrammarTaxonomyLoadResult.Loaded(it) }
            try {
                val taxonomy = source.getGrammarTaxonomy()
                cached = taxonomy
                GrammarTaxonomyLoadResult.Loaded(taxonomy)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                GrammarTaxonomyLoadResult.Failed(throwable)
            }
        }
    }
}

public sealed interface GrammarTaxonomyLoadResult {
    public data class Loaded(
        val taxonomy: GrammarTaxonomyDto,
    ) : GrammarTaxonomyLoadResult

    public data class Failed(
        val cause: Throwable,
    ) : GrammarTaxonomyLoadResult
}
