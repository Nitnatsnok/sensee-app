package app.sensee.grammar.domain

/**
 * Outcome of [GrammarLabelsProvider.awaitLabels] — splits "load failed" from
 * "the source returned an empty dictionary", which the swallow-to-`EMPTY`
 * shape of [GrammarLabelsProvider.labels] cannot.
 */
public sealed interface GrammarLabelsLoadResult {
    public data class Loaded(
        val labels: GrammarLabels,
    ) : GrammarLabelsLoadResult

    public data class Failed(
        val cause: Throwable?,
    ) : GrammarLabelsLoadResult
}
