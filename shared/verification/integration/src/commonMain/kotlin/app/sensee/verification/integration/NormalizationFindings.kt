package app.sensee.verification.integration

import app.sensee.verification.core.contract.Finding
import app.sensee.verification.core.contract.FindingSeverity
import app.sensee.verification.core.contract.FindingTarget
import app.sensee.verification.core.contract.LexicalSourceRef
import app.sensee.verification.core.contract.NormalizationKind
import app.sensee.verification.core.contract.NormalizationOutcome
import app.sensee.verification.core.contract.SuggestedAction

internal fun normalizationFindings(normalization: NormalizationOutcome): List<Finding> {
    val candidates =
        normalization.candidates.distinctBy { candidate ->
            NormalizationFindingKey(
                kind = candidate.kind,
                text = candidate.text,
                source = candidate.source,
            )
        }
    return candidates.map { candidate ->
        Finding(
            code = candidate.kind.findingCode(),
            severity = candidate.kind.findingSeverity(),
            target = FindingTarget.Headword,
            message = candidate.kind.findingMessage(candidate.text),
            sources = listOf(candidate.source),
            suggestedActions = listOf(SuggestedAction.ReplaceHeadword(candidate.text)),
        )
    }
}

private data class NormalizationFindingKey(
    val kind: NormalizationKind,
    val text: String,
    val source: LexicalSourceRef,
)

private fun NormalizationKind.findingCode(): String =
    when (this) {
        NormalizationKind.Lemma -> "LEMMA_NORMALIZED"
        NormalizationKind.Headword -> "HEADWORD_NORMALIZED"
        NormalizationKind.SpellFix -> "SPELLING_NORMALIZED"
        NormalizationKind.CaseFix -> "CASE_NORMALIZED"
        NormalizationKind.VariantForm -> "VARIANT_NORMALIZED"
        NormalizationKind.ParticleForm -> "PARTICLE_FORM_NORMALIZED"
    }

private fun NormalizationKind.findingSeverity(): FindingSeverity =
    when (this) {
        NormalizationKind.SpellFix -> FindingSeverity.Warning
        NormalizationKind.Lemma,
        NormalizationKind.Headword,
        NormalizationKind.CaseFix,
        NormalizationKind.VariantForm,
        NormalizationKind.ParticleForm,
        -> FindingSeverity.Info
    }

private fun NormalizationKind.findingMessage(text: String): String =
    when (this) {
        NormalizationKind.Lemma -> "Suggested lemma: $text"
        NormalizationKind.Headword -> "Suggested headword: $text"
        NormalizationKind.SpellFix -> "Suggested spelling: $text"
        NormalizationKind.CaseFix -> "Suggested capitalization: $text"
        NormalizationKind.VariantForm -> "Suggested variant form: $text"
        NormalizationKind.ParticleForm -> "Suggested particle form: $text"
    }
