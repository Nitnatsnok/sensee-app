package app.sensee.feature.startup.domain

import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarLabelsProvider
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Preloads every long-lived runtime dictionary in parallel and reports which
 * landed. Never throws — provider failures degrade the cache to `EMPTY`/`null`
 * and surface here as a `false` flag in [PreloadOutcome], so the splash can
 * either proceed or offer a retry without writing its own try/catch.
 *
 * Adding a new dictionary means adding it here; call sites stay as they are.
 */
@Inject
public class PreloadAppStartupUseCase(
    private val grammarLabels: GrammarLabelsProvider,
    private val taxonomyInvariants: TaxonomyInvariantsProvider,
) {
    public suspend operator fun invoke(): PreloadOutcome =
        coroutineScope {
            val labels = async { grammarLabels.labels() }
            val invariants = async { taxonomyInvariants.invariants() }
            PreloadOutcome(
                grammarLabelsLoaded = labels.await() !== GrammarLabels.EMPTY,
                taxonomyInvariantsLoaded = invariants.await() != null,
            )
        }
}
