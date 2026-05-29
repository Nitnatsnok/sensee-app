package app.sensee.ai.core

/**
 * CEFR level of a sense, surfaced through [EnrichmentSuggestion.extensions]
 * rather than the wire DTO: reference data a provider may attach, not part of
 * the neutral sense model. The curated layer authors it per fixture; the LLM
 * may estimate it from the field guidance. Consumers should prefer an
 * authoritative dictionary level over a model estimate.
 */
public object CefrEnrichmentExtension : AiEnrichmentExtension {
    public const val KEY: String = "cefr"

    override val id: String = KEY
    override val ownedKeys: Set<String> = setOf(KEY)

    override fun fields(): List<EnrichmentSchema.Field> =
        listOf(
            EnrichmentSchema.Field(
                serialName = KEY,
                shape = EnrichmentSchema.ShapeType.Text,
                guidance =
                    "CEFR level of THIS sense for a learner: one of " +
                        "A1, A2, B1, B2, C1, C2; omit if unsure",
            ),
        )
}

/** Extensions contributed to the AI-enrichment seam by default (ADR-006). */
public val DefaultAiEnrichmentExtensions: Set<AiEnrichmentExtension> = setOf(CefrEnrichmentExtension)
