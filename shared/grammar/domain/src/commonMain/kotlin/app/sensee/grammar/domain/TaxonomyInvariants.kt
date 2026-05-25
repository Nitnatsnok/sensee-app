package app.sensee.grammar.domain

/**
 * Flat snapshot of the loaded backend taxonomy. The AI schema builder reads
 * the sets to constrain the structured-output JSON; [GrammarTag.resolve] and
 * [UsageLabel.resolve] use the same shape to validate AI suggestions. [EMPTY]
 * is the safe degraded value — strict resolvers drop every pair, the schema
 * collapses to free strings/objects.
 */
public data class TaxonomyInvariants(
    val knownUnitTypeIds: Set<String>,
    val knownComplementIds: Set<String>,
    val allowedValuesByAxis: Map<String, Set<String>>,
    val allowedFormsByCategory: Map<String, Set<String>>,
) {
    public companion object {
        public val EMPTY: TaxonomyInvariants =
            TaxonomyInvariants(
                knownUnitTypeIds = emptySet(),
                knownComplementIds = emptySet(),
                allowedValuesByAxis = emptyMap(),
                allowedFormsByCategory = emptyMap(),
            )
    }
}
