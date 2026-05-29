package app.sensee.ai.core

/**
 * Flat snapshot of the runtime lexical taxonomy that constrains
 * [EnrichmentSchema.buildJsonSchema]. Lives in `core` with no `grammar`
 * dependency so the seam boundary stays provider- and feature-agnostic
 * (ADR-005); the LLM client maps `TaxonomyInvariants` into this at the call
 * site. Empty slices collapse the matching constraint to its structural shape
 * (degraded mode — a failed taxonomy fetch still yields a usable schema).
 */
public data class EnrichmentTaxonomy(
    val unitTypeIds: Set<String> = emptySet(),
    val complementIds: Set<String> = emptySet(),
    val usageAxesAndValues: Map<String, Set<String>> = emptyMap(),
    val grammarCategoriesAndForms: Map<String, Set<String>> = emptyMap(),
) {
    public companion object {
        public val EMPTY: EnrichmentTaxonomy = EnrichmentTaxonomy()
    }
}
