package app.sensee.feature.vocabularyEditor.data

import app.sensee.feature.vocabularyEditor.domain.ContextualApplication
import app.sensee.feature.vocabularyEditor.domain.Meaning
import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.PrepositionGovernment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.grammar.domain.UsageAxis
import app.sensee.grammar.domain.UsageLabel
import app.sensee.grammar.domain.UsageValue
import kotlinx.serialization.Serializable

/**
 * The persisted shape of a confirmed [Meaning]. Domain types (SurfaceForm,
 * StudiedSentence, sealed grammar interfaces) are not serializable, so this
 * DTO mirrors them as ids/markers and the mappers re-resolve through the same
 * neutral `parse`/`fromId` the AI boundary uses — no second source of truth.
 * On read the mapper is **pass-through** (an id outside the sealed set
 * surfaces as `Unknown(id)`, never dropped): the values were written through
 * the strict AI-boundary mapper, so we trust them; pass-through also keeps
 * stored data forward-compatible across taxonomy growth.
 */
@Serializable
internal data class MeaningDto(
    val translation: String,
    val surfaceForm: String? = null,
    val unitType: String? = null,
    val baseLemma: String? = null,
    val explanation: String? = null,
    val contextualApplications: List<String> = emptyList(),
    val governedPrepositions: List<PrepositionGovernmentDto> = emptyList(),
    val complementation: List<String> = emptyList(),
    val usageLabels: List<UsageLabelDto> = emptyList(),
    val usageNote: String? = null,
    val grammarTags: List<GrammarTagDto> = emptyList(),
    val irregularForms: IrregularFormsDto? = null,
)

@Serializable
internal data class PrepositionGovernmentDto(
    val alternatives: List<String>,
    val example: String? = null,
)

@Serializable
internal data class UsageLabelDto(
    val axis: String,
    val value: String,
)

@Serializable
internal data class GrammarTagDto(
    val category: String,
    val form: String,
)

@Serializable
internal data class IrregularFormsDto(
    val base: String,
    val past: String,
    val pastParticiple: String,
)

internal fun Meaning.toDto(): MeaningDto =
    MeaningDto(
        translation = translation,
        surfaceForm = surfaceForm?.display(),
        unitType = unitType?.id,
        baseLemma = baseLemma,
        explanation = explanation,
        contextualApplications = contextualApplications.map { it.sentence.marked() },
        governedPrepositions =
            governedPrepositions.map { PrepositionGovernmentDto(it.alternatives, it.example) },
        complementation = complementation.map { it.id },
        usageLabels = usageLabels.map { UsageLabelDto(it.axis.id, it.value.id) },
        usageNote = usageNote,
        grammarTags = grammarTags.map { GrammarTagDto(it.category.id, it.form.id) },
        irregularForms =
            irregularForms?.let { IrregularFormsDto(it.base, it.past, it.pastParticiple) },
    )

internal fun MeaningDto.toDomain(): Meaning =
    Meaning(
        translation = translation,
        surfaceForm = surfaceForm?.takeIf { it.isNotBlank() }?.let(SurfaceForm::parse),
        unitType = unitType?.let { GrammarUnitType.fromId(it) },
        baseLemma = baseLemma,
        explanation = explanation,
        contextualApplications =
            contextualApplications.map { ContextualApplication(StudiedSentence.parse(it)) },
        governedPrepositions =
            governedPrepositions.map { PrepositionGovernment(it.alternatives, it.example) },
        complementation = complementation.map(ComplementType::fromId),
        usageLabels =
            usageLabels.map { UsageLabel(UsageAxis.fromId(it.axis), UsageValue.fromId(it.value)) },
        usageNote = usageNote,
        grammarTags =
            grammarTags.map { GrammarTag(GrammarCategory.fromId(it.category), GrammarForm.fromId(it.form)) },
        irregularForms =
            irregularForms?.let { IrregularForms(it.base, it.past, it.pastParticiple) },
    )
