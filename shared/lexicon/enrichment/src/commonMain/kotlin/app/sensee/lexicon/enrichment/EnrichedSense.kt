package app.sensee.lexicon.enrichment

import app.sensee.lexicon.domain.Sense

/**
 * A [Sense] produced from enrichment plus a stable fingerprint of enrichment
 * details that are useful for candidate identity but are not part of confirmed
 * sense deduplication.
 */
public data class EnrichedSense(
    val sense: Sense,
    val contentFingerprint: String,
)
