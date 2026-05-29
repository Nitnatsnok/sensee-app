package app.sensee.grammar.data

import app.sensee.grammar.domain.TaxonomyInvariants
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/**
 * App-scoped [TaxonomyInvariantsProvider] backed by the shared taxonomy cache.
 * Owns only the invariants projection — preload lives in
 * `PreloadAppStartupUseCase`.
 */
@SingleIn(AppScope::class)
@ContributesBinding(scope = AppScope::class, binding = binding<TaxonomyInvariantsProvider>())
@Inject
public class CachingTaxonomyInvariantsProvider(
    private val taxonomyProvider: CachingGrammarTaxonomyProvider,
) : TaxonomyInvariantsProvider {
    private val mutex = Mutex()

    @Volatile
    private var cached: TaxonomyInvariants? = null

    override suspend fun invariants(): TaxonomyInvariants? {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: load()?.also { cached = it }
        }
    }

    override fun cachedInvariants(): TaxonomyInvariants? = cached

    private suspend fun load(): TaxonomyInvariants? =
        when (val result = taxonomyProvider.taxonomy()) {
            is GrammarTaxonomyLoadResult.Loaded -> result.taxonomy.toTaxonomyInvariants()
            is GrammarTaxonomyLoadResult.Failed -> null
        }
}
