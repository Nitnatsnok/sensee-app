package app.sensee.feature.vocabularyEditor.domain

import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.PrepositionGovernment
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.grammar.domain.UnitComponent
import app.sensee.grammar.domain.UsageLabel
import app.sensee.lexicon.domain.ContextualApplication
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.WordFamilyMember
import kotlin.jvm.JvmInline
import app.sensee.lexicon.domain.deriveSenseContentKey as deriveLexiconSenseContentKey

/**
 * Stable identity of a [SenseCandidate] within a capture session. Derived
 * from the candidate's content (see [deriveSenseCandidateId]), so a re-enrich
 * that returns the same sense yields the same id — selection survives a
 * candidate-list replacement instead of silently shifting with list indices.
 */
@JvmInline
public value class SenseCandidateId(
    public val value: String,
)

/**
 * An unconfirmed proposal (ADR-001). It is never silently promoted to a
 * [Sense]; the user explicitly selects it (the selection is the
 * confirmation). Provenance is implicit: every candidate is AI/assistant
 * proposed and unverified until selected. [id] is a content-derived stable
 * identity used to key selection.
 */
public data class SenseCandidate(
    val id: SenseCandidateId,
    val sense: Sense,
    val verification: SenseVerificationSnapshot? = null,
) {
    public constructor(
        id: SenseCandidateId,
        translation: String,
        surfaceForm: SurfaceForm? = null,
        unitType: GrammarUnitType? = null,
        baseLemma: String? = null,
        headLemma: String? = null,
        components: List<UnitComponent> = emptyList(),
        explanation: String? = null,
        contextualApplications: List<ContextualApplication> = emptyList(),
        governedPrepositions: List<PrepositionGovernment> = emptyList(),
        complementation: List<ComplementType> = emptyList(),
        usageLabels: List<UsageLabel> = emptyList(),
        usageNote: String? = null,
        grammarTags: List<GrammarTag> = emptyList(),
        irregularForms: IrregularForms? = null,
        synonyms: List<String> = emptyList(),
        antonyms: List<String> = emptyList(),
        collocations: List<String> = emptyList(),
        wordFamily: List<WordFamilyMember> = emptyList(),
        verification: SenseVerificationSnapshot? = null,
    ) : this(
        id = id,
        sense =
            Sense(
                translation = translation,
                surfaceForm = surfaceForm,
                unitType = unitType,
                baseLemma = baseLemma,
                headLemma = headLemma,
                components = components,
                explanation = explanation,
                contextualApplications = contextualApplications,
                governedPrepositions = governedPrepositions,
                complementation = complementation,
                usageLabels = usageLabels,
                usageNote = usageNote,
                grammarTags = grammarTags,
                irregularForms = irregularForms,
                synonyms = synonyms,
                antonyms = antonyms,
                collocations = collocations,
                wordFamily = wordFamily,
            ),
        verification = verification,
    )

    public val translation: String get() = sense.translation
    public val surfaceForm: SurfaceForm? get() = sense.surfaceForm
    public val unitType: GrammarUnitType? get() = sense.unitType
    public val baseLemma: String? get() = sense.baseLemma
    public val headLemma: String? get() = sense.headLemma
    public val components: List<UnitComponent> get() = sense.components
    public val explanation: String? get() = sense.explanation
    public val contextualApplications: List<ContextualApplication> get() = sense.contextualApplications
    public val governedPrepositions: List<PrepositionGovernment> get() = sense.governedPrepositions
    public val complementation: List<ComplementType> get() = sense.complementation
    public val usageLabels: List<UsageLabel> get() = sense.usageLabels
    public val usageNote: String? get() = sense.usageNote
    public val grammarTags: List<GrammarTag> get() = sense.grammarTags
    public val irregularForms: IrregularForms? get() = sense.irregularForms
    public val synonyms: List<String> get() = sense.synonyms
    public val antonyms: List<String> get() = sense.antonyms
    public val collocations: List<String> get() = sense.collocations
    public val wordFamily: List<WordFamilyMember> get() = sense.wordFamily

    public fun toConfirmedSense(): Sense = sense

    public fun toConfirmedMeaning(): Meaning = sense
}

/**
 * Derives a [SenseCandidateId] from the fields that identify a sense - the
 * surface form, part of speech, translation and an optional content
 * fingerprint. Stable across a re-enrich for the same sense; distinct senses
 * of one term differ by their identifying content.
 */
public fun deriveSenseCandidateId(
    translation: String,
    surfaceForm: SurfaceForm?,
    unitType: GrammarUnitType?,
    contentFingerprint: String = "",
): SenseCandidateId =
    SenseCandidateId(
        listOf(
            surfaceForm?.display().orEmpty(),
            unitType?.id.orEmpty(),
            translation,
            contentFingerprint,
        ).joinToString(separator = "|", transform = ::stableIdPart),
    )

private fun stableIdPart(value: String): String = "${value.length}:$value"

public fun deriveMeaningCandidateId(
    translation: String,
    surfaceForm: SurfaceForm?,
    unitType: GrammarUnitType?,
    contentFingerprint: String = "",
): MeaningCandidateId =
    deriveSenseCandidateId(
        translation = translation,
        surfaceForm = surfaceForm,
        unitType = unitType,
        contentFingerprint = contentFingerprint,
    )

/**
 * Content-derived dedup key for a confirmed [Sense]. Two captures that yield
 * an equal key are the same sense — repository merge keeps the first and drops
 * the rest so a re-capture does not duplicate the user's existing senses or
 * orphan their SRS state. Identity covers what disambiguates the sense for the
 * learner: the surface form (with its slots), the part-of-speech tag and the
 * native-language translation. Verifier metadata such as [Sense.headLemma] and
 * [Sense.baseLemma], plus wording variations in [Sense.explanation], do NOT
 * change identity; enriching a previously manual/degraded sense must not create
 * a duplicate.
 */
public fun deriveSenseContentKey(sense: Sense): String = deriveLexiconSenseContentKey(sense)
