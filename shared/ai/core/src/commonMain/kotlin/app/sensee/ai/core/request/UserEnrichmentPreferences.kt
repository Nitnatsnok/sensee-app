package app.sensee.ai.core.request

/**
 * Slow-changing learner preferences that steer enrichment without changing
 * the wire schema. New options live here, not on [EnrichmentRequest] (which
 * carries per-call input, not stored preferences).
 */
public data class UserEnrichmentPreferences(
    val includePhrasalVerbs: Boolean = false,
) {
    public companion object {
        public val EMPTY: UserEnrichmentPreferences = UserEnrichmentPreferences()
    }
}

public fun interface UserEnrichmentPreferencesProvider {
    public suspend fun preferences(): UserEnrichmentPreferences
}
