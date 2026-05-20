package app.sensee.grammar.data

import app.sensee.grammar.domain.GrammarLabels
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * App-scoped access to the grammar label dictionary. The taxonomy is stable for
 * the app session, so it is fetched once and shared by every consumer (capture
 * now; library/practice label rendering later) instead of each refetching.
 *
 * Warmed at startup but safe to call anytime: a failed load returns
 * [GrammarLabels.EMPTY] (ids fall back, nothing blocks) and is NOT cached, so a
 * later call retries; only a successful load is memoized.
 */
public interface GrammarLabelsProvider {
    public suspend fun labels(): GrammarLabels
}

@SingleIn(AppScope::class)
@ContributesBinding(scope = AppScope::class, binding = binding<GrammarLabelsProvider>())
@Inject
public class WarmingGrammarLabelsProvider(
    private val source: GrammarTaxonomySource,
) : GrammarLabelsProvider {
    private val mutex = Mutex()
    private var cached: GrammarLabels? = null

    override suspend fun labels(): GrammarLabels {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: load().also { resolved ->
                if (resolved !== GrammarLabels.EMPTY) cached = resolved
            }
        }
    }

    private suspend fun load(): GrammarLabels =
        try {
            source.getGrammarTaxonomy().toGrammarLabels()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            GrammarLabels.EMPTY
        }
}
