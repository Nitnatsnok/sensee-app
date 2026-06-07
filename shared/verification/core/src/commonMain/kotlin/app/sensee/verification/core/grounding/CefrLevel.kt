package app.sensee.verification.core.grounding

import kotlinx.serialization.Serializable

/**
 * Intentional local mirror of `lexicon.domain.CefrLevel`: the verification seam
 * keeps its own copy so `verification.core` stays a dependency-free leaf and never
 * depends on `lexicon`. Same six CEFR levels; values are mapped at the seam edge.
 */
@Serializable
public enum class CefrLevel {
    A1,
    A2,
    B1,
    B2,
    C1,
    C2,
}
