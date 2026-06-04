package app.sensee.verification.core.hierarchy

import app.sensee.verification.core.contract.LexicalEntryTypeHint
import app.sensee.verification.core.contract.LexicalSourceRef
import app.sensee.verification.core.contract.VerificationPolicy
import app.sensee.verification.core.grounding.CefrLevel
import kotlinx.serialization.Serializable

/**
 * Family-graph evidence for one query: what unit the verifier understood the
 * input as, who its head lemma is, and which sibling units share that head.
 * Populated only when [VerificationPolicy.includeFamily] is on — an empty
 * report means "policy did not ask", not "no family".
 */
@Serializable
public data class FamilyContext(
    val resolvedUnit: LexicalUnitInfo? = null,
    val headLemma: LexicalLemma? = null,
    val siblings: List<LexicalUnitSummary> = emptyList(),
    val didYouMean: List<LexicalUnitSummary> = emptyList(),
    val truncated: Boolean = false,
)

/**
 * Lightweight projection of a [LexicalUnitInfo] for family list rendering and
 * "did you mean?" hints. Carries only what the orchestrator needs to label
 * the sibling — full unit metadata is fetched on demand.
 */
@Serializable
public data class LexicalUnitSummary(
    val id: LexicalUnitId,
    val displayForm: String,
    val entryType: LexicalEntryTypeHint,
    val shortLabel: String? = null,
    val cefr: CefrLevel? = null,
    val sources: List<LexicalSourceRef> = emptyList(),
)
