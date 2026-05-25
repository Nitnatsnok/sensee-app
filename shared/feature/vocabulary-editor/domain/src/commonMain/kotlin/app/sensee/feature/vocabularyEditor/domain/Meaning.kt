package app.sensee.feature.vocabularyEditor.domain

import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.PrepositionGovernment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.grammar.domain.UsageLabel
import kotlin.jvm.JvmInline

/**
 * Stable identity of a [MeaningCandidate] within a capture session. Derived
 * from the candidate's content (see [deriveMeaningCandidateId]), so a re-enrich
 * that returns the same sense yields the same id — selection survives a
 * candidate-list replacement instead of silently shifting with list indices.
 */
@JvmInline
public value class MeaningCandidateId(
    public val value: String,
)

/**
 * One usage example of a sense. A sense carries a list of these (one per usage
 * variant), never a single merged sentence (ADR-001). The [sentence] is
 * structured so the studied unit is explicitly marked — practice highlights /
 * cloze-masks it without fragile substring matching.
 */
public data class ContextualApplication(
    val sentence: StudiedSentence,
)

/**
 * A confirmed sense of an entry. Becomes eligible for derivation into practice
 * cards once the entry is [EntryStatus.Confirmed]. Identity of a sense includes
 * its [surfaceForm]; part of speech ([unitType]) is a property of the sense, not
 * of the input. [governedPrepositions] is a structured collocation cue (ADR-001),
 * never free text folded into [explanation].
 */
public data class Meaning(
    val translation: String,
    val surfaceForm: SurfaceForm? = null,
    val unitType: GrammarUnitType? = null,
    val baseLemma: String? = null,
    val explanation: String? = null,
    val contextualApplications: List<ContextualApplication> = emptyList(),
    val governedPrepositions: List<PrepositionGovernment> = emptyList(),
    val complementation: List<ComplementType> = emptyList(),
    val usageLabels: List<UsageLabel> = emptyList(),
    val usageNote: String? = null,
    val grammarTags: List<GrammarTag> = emptyList(),
    val irregularForms: IrregularForms? = null,
)

/**
 * An unconfirmed proposal (ADR-001). It is never silently promoted to a
 * [Meaning]; the user explicitly selects it (the selection is the
 * confirmation). Provenance is implicit: every candidate is AI/assistant
 * proposed and unverified until selected. [id] is a content-derived stable
 * identity used to key selection.
 */
public data class MeaningCandidate(
    val id: MeaningCandidateId,
    val translation: String,
    val surfaceForm: SurfaceForm? = null,
    val unitType: GrammarUnitType? = null,
    val baseLemma: String? = null,
    val explanation: String? = null,
    val contextualApplications: List<ContextualApplication> = emptyList(),
    val governedPrepositions: List<PrepositionGovernment> = emptyList(),
    val complementation: List<ComplementType> = emptyList(),
    val usageLabels: List<UsageLabel> = emptyList(),
    val usageNote: String? = null,
    val grammarTags: List<GrammarTag> = emptyList(),
    val irregularForms: IrregularForms? = null,
) {
    public fun toConfirmedMeaning(): Meaning =
        Meaning(
            translation = translation,
            surfaceForm = surfaceForm,
            unitType = unitType,
            baseLemma = baseLemma,
            explanation = explanation,
            contextualApplications = contextualApplications,
            governedPrepositions = governedPrepositions,
            complementation = complementation,
            usageLabels = usageLabels,
            usageNote = usageNote,
            grammarTags = grammarTags,
            irregularForms = irregularForms,
        )
}

/**
 * Derives a [MeaningCandidateId] from the fields that identify a sense - the
 * surface form, part of speech, translation and an optional content
 * fingerprint. Stable across a re-enrich for the same sense; distinct senses
 * of one term differ by their identifying content.
 */
public fun deriveMeaningCandidateId(
    translation: String,
    surfaceForm: SurfaceForm?,
    unitType: GrammarUnitType?,
    contentFingerprint: String = "",
): MeaningCandidateId =
    MeaningCandidateId(
        listOf(
            surfaceForm?.display().orEmpty(),
            unitType?.id.orEmpty(),
            translation,
            contentFingerprint,
        ).joinToString(separator = "|", transform = ::stableIdPart),
    )

private fun stableIdPart(value: String): String = "${value.length}:$value"
