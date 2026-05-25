package app.sensee.grammar.domain

/**
 * App-scoped reader for the grammar label dictionary. Three APIs:
 *
 * - [labels] — suspending fetch; degrades to [GrammarLabels.EMPTY] on
 *   failure, swallows the cause.
 * - [cachedLabels] — synchronous snapshot for UI init.
 * - [awaitLabels] — like [labels] but returns a sealed result so a screen
 *   can render loading/error/retry instead of a silent `EMPTY`.
 *
 * Lives in `domain` so feature presentation can read snapshots without
 * depending on `data`. The companion [TaxonomyInvariantsProvider] uses
 * `null` for "not loaded" because the AI-boundary mapper switches modes on
 * that signal (ADR-006).
 */
public fun interface GrammarLabelsProvider {
    public suspend fun labels(): GrammarLabels

    /** Caching impl overrides this; default is `EMPTY` for tests. */
    public fun cachedLabels(): GrammarLabels = GrammarLabels.EMPTY

    /** Caching impl overrides this; the default cannot recover the cause. */
    public suspend fun awaitLabels(): GrammarLabelsLoadResult {
        val resolved = labels()
        return if (resolved === GrammarLabels.EMPTY) {
            GrammarLabelsLoadResult.Failed(cause = null)
        } else {
            GrammarLabelsLoadResult.Loaded(resolved)
        }
    }
}
