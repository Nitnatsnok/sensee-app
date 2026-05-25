package app.sensee.feature.startup.domain

/**
 * Outcome of [PreloadAppStartupUseCase]. A `false` flag means the provider
 * failed and degraded its cache (`EMPTY` for labels, `null` for invariants);
 * the splash treats any failure as a single retry-able state — granular
 * per-dictionary retry would not change the user's action.
 */
public data class PreloadOutcome(
    val grammarLabelsLoaded: Boolean,
    val taxonomyInvariantsLoaded: Boolean,
) {
    public val isFullSuccess: Boolean
        get() = grammarLabelsLoaded && taxonomyInvariantsLoaded

    public val hasAnyFailure: Boolean
        get() = !isFullSuccess
}
