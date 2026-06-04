package app.sensee.verification.core.contract

import kotlinx.serialization.Serializable

/**
 * What the verifier thinks the input should be in canonical form. [canonical]
 * is a chosen value (consensus across providers when they agree); the full
 * per-source list rides in [candidates] so the orchestrator can show "we
 * normalized X → Y because <source>".
 *
 * The seam never silently rewrites the term — it returns the candidate plus
 * a [Finding] for the orchestrator to render or apply.
 */
@Serializable
public data class NormalizationOutcome(
    val canonical: String? = null,
    val candidates: List<NormalizationCandidate> = emptyList(),
) {
    public companion object {
        public val EMPTY: NormalizationOutcome = NormalizationOutcome()
    }
}

@Serializable
public data class NormalizationCandidate(
    val text: String,
    val kind: NormalizationKind,
    val source: LexicalSourceRef,
    val confidence: Confidence,
)

@Serializable
public enum class NormalizationKind {
    Lemma,
    Headword,
    SpellFix,
    CaseFix,
    VariantForm,
    ParticleForm,
}
