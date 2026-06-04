package app.sensee.verification.core.grounding

import app.sensee.verification.core.contract.Confidence
import app.sensee.verification.core.contract.LexicalSourceRef
import app.sensee.verification.core.contract.LicensePolicy
import app.sensee.verification.core.contract.PartOfSpeechHint
import kotlinx.serialization.Serializable

/**
 * Verifier's view of the dictionary sense inventory for a term. The seam no
 * longer takes AI senses as input, so it does not attempt AI-to-dictionary
 * matching; it only surfaces the dictionary senses it knows in
 * [extraDictionarySenses] for an optional "you may also want…" grounding
 * surface. [confidence] reflects the contributing inventory provider.
 */
@Serializable
public data class SenseMapping(
    val extraDictionarySenses: List<DictionarySenseSummary> = emptyList(),
    val confidence: Confidence = Confidence.Low,
) {
    public companion object {
        public val EMPTY: SenseMapping = SenseMapping()
    }
}

@Serializable
public data class DictionarySenseSummary(
    val ref: LexicalSourceRef,
    val pos: PartOfSpeechHint? = null,
    /**
     * Short safe-to-store label. Carrying a full gloss verbatim is gated by
     * the source's [LicensePolicy.storeContentAllowed]; adapters that cannot
     * store glosses leave this `null` and the orchestrator renders by ref
     * only.
     */
    val shortLabel: String? = null,
    val cefr: CefrLevel? = null,
)
