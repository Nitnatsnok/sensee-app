package app.sensee.lexicon.serialization

import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.ComponentRole
import app.sensee.grammar.domain.ComponentSalience
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.PrepositionGovernment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.grammar.domain.UnitComponent
import app.sensee.grammar.domain.UsageAxis
import app.sensee.grammar.domain.UsageLabel
import app.sensee.grammar.domain.UsageValue
import app.sensee.lexicon.domain.AlignmentChunk
import app.sensee.lexicon.domain.CefrLevel
import app.sensee.lexicon.domain.ContextualApplication
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.WordFamilyMember
import kotlinx.serialization.Serializable

/**
 * The persisted shape of a [Sense]. Domain types (SurfaceForm, StudiedSentence,
 * sealed grammar interfaces) are not serializable, so this DTO mirrors them as
 * ids/markers and the mappers re-resolve through the same neutral `parse`/`fromId`
 * the AI boundary uses — no second source of truth. This is an intentional
 * boundary mirror of [Sense] on the persist side: the duplication is deliberate,
 * do not collapse it into the domain type. On read the mapper is
 * **pass-through** (an id outside the sealed set surfaces as `Unknown(id)`, never
 * dropped): the values were written through the strict AI-boundary mapper, so we
 * trust them; pass-through also keeps stored data forward-compatible across
 * taxonomy growth.
 *
 * Shared by every feature that persists a confirmed sense (capture and the
 * catalog), so a user-captured sense and a service-deck sense are the same
 * stored shape.
 */
@Serializable
public data class SenseDto(
    val translation: String,
    val surfaceForm: String? = null,
    val unitType: String? = null,
    val baseLemma: String? = null,
    val headLemma: String? = null,
    val components: List<UnitComponentDto> = emptyList(),
    val explanation: String? = null,
    val contextualApplications: List<ContextualApplicationDto> = emptyList(),
    val governedPrepositions: List<PrepositionGovernmentDto> = emptyList(),
    val complementation: List<String> = emptyList(),
    val usageLabels: List<UsageLabelDto> = emptyList(),
    val usageNote: String? = null,
    val grammarTags: List<GrammarTagDto> = emptyList(),
    val irregularForms: IrregularFormsDto? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val wordFamily: List<WordFamilyMemberDto> = emptyList(),
    /**
     * CEFR level as a wire string (forward-compatible: an unrecognised value
     * resolves to `null` on read via [CefrLevel.fromId], never a parse failure).
     */
    val cefr: String? = null,
)

@Serializable
public data class UnitComponentDto(
    val text: String,
    val role: String,
    val salience: String? = null,
)

@Serializable
public data class WordFamilyMemberDto(
    val lemma: String,
    val unitType: String? = null,
)

@Serializable
public data class ContextualApplicationDto(
    val sentence: String,
    val translation: String? = null,
    val alignment: List<AlignmentChunkDto> = emptyList(),
)

/** Persist rep of an alignment chunk; one of the four intentional boundary mirrors (see [SenseDto]). */
@Serializable
public data class AlignmentChunkDto(
    val source: String,
    val target: String,
)

@Serializable
public data class PrepositionGovernmentDto(
    val alternatives: List<String>,
    val example: String? = null,
)

@Serializable
public data class UsageLabelDto(
    val axis: String,
    val value: String,
)

@Serializable
public data class GrammarTagDto(
    val category: String,
    val form: String,
)

@Serializable
public data class IrregularFormsDto(
    val base: String,
    val past: String,
    val pastParticiple: String,
)

public fun Sense.toDto(): SenseDto =
    SenseDto(
        translation = translation,
        surfaceForm = surfaceForm?.display(),
        unitType = unitType?.id,
        baseLemma = baseLemma,
        headLemma = headLemma,
        components = components.map { UnitComponentDto(it.text, it.role.id, it.salience?.id) },
        explanation = explanation,
        contextualApplications =
            contextualApplications.map {
                ContextualApplicationDto(
                    sentence = it.sentence.marked(),
                    translation = it.translation,
                    alignment = it.alignment.map { c -> AlignmentChunkDto(c.source, c.target) },
                )
            },
        governedPrepositions =
            governedPrepositions.map { PrepositionGovernmentDto(it.alternatives, it.example) },
        complementation = complementation.map { it.id },
        usageLabels = usageLabels.map { UsageLabelDto(it.axis.id, it.value.id) },
        usageNote = usageNote,
        grammarTags = grammarTags.map { GrammarTagDto(it.category.id, it.form.id) },
        irregularForms =
            irregularForms?.let { IrregularFormsDto(it.base, it.past, it.pastParticiple) },
        synonyms = synonyms,
        antonyms = antonyms,
        collocations = collocations,
        wordFamily = wordFamily.map { WordFamilyMemberDto(it.lemma, it.unitType?.id) },
        cefr = cefr?.name,
    )

public fun SenseDto.toDomain(): Sense =
    Sense(
        translation = translation,
        surfaceForm = surfaceForm?.takeIf { it.isNotBlank() }?.let(SurfaceForm::parse),
        unitType = unitType?.let { GrammarUnitType.fromId(it) },
        baseLemma = baseLemma,
        headLemma = headLemma,
        components =
            components.map {
                UnitComponent(
                    it.text,
                    ComponentRole.fromId(it.role),
                    it.salience?.let(ComponentSalience::fromId),
                )
            },
        explanation = explanation,
        contextualApplications =
            contextualApplications.map {
                ContextualApplication(
                    sentence = StudiedSentence.parse(it.sentence),
                    translation = it.translation,
                    alignment = it.alignment.map { c -> AlignmentChunk(source = c.source, target = c.target) },
                )
            },
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
        synonyms = synonyms,
        antonyms = antonyms,
        collocations = collocations,
        wordFamily = wordFamily.map { WordFamilyMember(it.lemma, it.unitType?.let(GrammarUnitType::fromId)) },
        cefr = CefrLevel.fromId(cefr),
    )
