package app.sensee.grammar.data

import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarLabelsLoadResult
import app.sensee.grammar.domain.GrammarLabelsProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/**
 * App-scoped [GrammarLabelsProvider] backed by the shared taxonomy cache.
 * Owns only the labels projection — the preload trigger lives in
 * `PreloadAppStartupUseCase`.
 */
@SingleIn(AppScope::class)
@ContributesBinding(scope = AppScope::class, binding = binding<GrammarLabelsProvider>())
@Inject
public class CachingGrammarLabelsProvider(
    private val taxonomyProvider: CachingGrammarTaxonomyProvider,
) : GrammarLabelsProvider {
    private val mutex = Mutex()

    @Volatile
    private var cached: GrammarLabels? = null

    override suspend fun labels(): GrammarLabels =
        when (val result = loadOrFail()) {
            is GrammarLabelsLoadResult.Loaded -> result.labels
            is GrammarLabelsLoadResult.Failed -> GrammarLabels.EMPTY
        }

    override fun cachedLabels(): GrammarLabels = cached ?: GrammarLabels.EMPTY

    override suspend fun awaitLabels(): GrammarLabelsLoadResult = loadOrFail()

    private suspend fun loadOrFail(): GrammarLabelsLoadResult {
        cached?.let { return GrammarLabelsLoadResult.Loaded(it) }
        return mutex.withLock {
            cached?.let { return@withLock GrammarLabelsLoadResult.Loaded(it) }
            when (val result = taxonomyProvider.taxonomy()) {
                is GrammarTaxonomyLoadResult.Loaded ->
                    GrammarLabelsLoadResult.Loaded(
                        result.taxonomy.toGrammarLabels().also { cached = it },
                    )
                is GrammarTaxonomyLoadResult.Failed ->
                    GrammarLabelsLoadResult.Failed(cause = result.cause)
            }
        }
    }
}
