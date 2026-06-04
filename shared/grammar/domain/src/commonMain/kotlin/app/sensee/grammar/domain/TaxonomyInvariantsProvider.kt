package app.sensee.grammar.domain

/**
 * App-scoped reader for the loaded [TaxonomyInvariants]. Mirrors
 * [GrammarLabelsProvider] except that "not loaded" is `null`, not an empty
 * value — the AI-boundary enrichment mapper (`EnrichmentSuggestion.toSense`)
 * flips its strict / pass-through mode on exactly that signal (ADR-006).
 *
 * - [invariants] — canonical suspending fetch. Cached on success; a failed
 *   load returns `null` and is **not** cached, so a later call retries.
 * - [cachedInvariants] — synchronous snapshot for call sites that cannot
 *   suspend; `null` before the first successful load.
 */
public fun interface TaxonomyInvariantsProvider {
    public suspend fun invariants(): TaxonomyInvariants?

    /** Caching impl overrides this; default is `null` for tests. */
    public fun cachedInvariants(): TaxonomyInvariants? = null
}
