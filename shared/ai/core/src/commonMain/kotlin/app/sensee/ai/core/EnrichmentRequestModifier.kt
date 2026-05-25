package app.sensee.ai.core

/**
 * Request-side seam (ADR-005): tunes *what and how* the provider is asked,
 * without changing the response schema (that is [AiEnrichmentExtension]'s
 * job). The assembler groups modifiers by [phase] and orders by [id] within
 * a phase, so adding a new option means adding a new modifier — not editing
 * the client.
 */
public interface EnrichmentRequestModifier {
    public val id: String

    public val phase: PromptPhase

    public fun contribute(context: EnrichmentRequestContext): PromptContribution
}

/** Assembly order: phases left-to-right, alphabetic by [EnrichmentRequestModifier.id] within a phase. */
public enum class PromptPhase {
    Identity,
    Format,
    CrossCutting,
    Coverage,
    Shaping,
    Preferences,
}

/**
 * Output of one modifier. [systemFragments] carry the behavior rules; the
 * rarely-used [userFragments] are for content appended to the user message
 * (per-call hints tied to the term itself).
 */
public data class PromptContribution(
    val systemFragments: List<String> = emptyList(),
    val userFragments: List<String> = emptyList(),
) {
    public companion object {
        public val EMPTY: PromptContribution = PromptContribution()
    }
}

/**
 * What a modifier sees per call. Taxonomy is intentionally absent — passing
 * it would couple `shared/ai/core` to a grammar wire type (ADR-005 boundary).
 */
public data class EnrichmentRequestContext(
    val request: EnrichmentRequest,
    val preferences: UserEnrichmentPreferences,
)

/** Pure assembly: phase-then-id ordering, fragment concatenation, nothing else. */
public object EnrichmentPromptAssembler {
    public fun assemble(
        modifiers: Set<EnrichmentRequestModifier>,
        context: EnrichmentRequestContext,
    ): AssembledPrompt {
        val byPhase = modifiers.groupBy { it.phase }
        val systemFragments = mutableListOf<String>()
        val userFragments = mutableListOf<String>()
        PromptPhase.entries.forEach { phase ->
            val ordered = byPhase[phase].orEmpty().sortedBy { it.id }
            ordered.forEach { modifier ->
                val contribution = modifier.contribute(context)
                systemFragments += contribution.systemFragments
                userFragments += contribution.userFragments
            }
        }
        return AssembledPrompt(
            systemFragments = systemFragments.toList(),
            userFragments = userFragments.toList(),
        )
    }
}

/** Result of [EnrichmentPromptAssembler.assemble]. Empty lists are valid. */
public data class AssembledPrompt(
    val systemFragments: List<String>,
    val userFragments: List<String>,
)
