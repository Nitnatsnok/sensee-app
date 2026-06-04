package app.sensee.lexicon.domain

import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.PrepositionGovernment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.grammar.domain.UnitComponent
import app.sensee.grammar.domain.UsageLabel

/**
 * The central lexical model of the app (Sensee): one confirmed sense of a word
 * or phrase. Enrichment (the AI seam) is a producer that fills a [Sense] from
 * the user's input; capture and the catalog both build on it, and the
 * lemma/derivative graph is derived from it — never the other way round.
 *
 * Identity of a sense includes its [surfaceForm]; part of speech ([unitType])
 * is a property of the sense, not of the input. [governedPrepositions] is a
 * structured collocation cue (ADR-001), never free text folded into
 * [explanation].
 *
 * [headLemma] is the family root: for `come across` it is `come`, for the
 * single-word verb `come` it is also `come` (or `null` when unresolved). The
 * catalog groups cards by [headLemma] so all `come*` units share one lemma page
 * (ADR-001). [components] is the structural breakdown of a multi-word unit;
 * empty means single-word or unresolved.
 *
 * [cefr] is a learner-level attribute of the sense; it never participates in
 * sense identity (`deriveSenseContentKey`).
 */
public data class Sense(
    val translation: String,
    val surfaceForm: SurfaceForm? = null,
    val unitType: GrammarUnitType? = null,
    val baseLemma: String? = null,
    val headLemma: String? = null,
    val components: List<UnitComponent> = emptyList(),
    val explanation: String? = null,
    val contextualApplications: List<ContextualApplication> = emptyList(),
    val governedPrepositions: List<PrepositionGovernment> = emptyList(),
    val complementation: List<ComplementType> = emptyList(),
    val usageLabels: List<UsageLabel> = emptyList(),
    val usageNote: String? = null,
    val grammarTags: List<GrammarTag> = emptyList(),
    val irregularForms: IrregularForms? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val wordFamily: List<WordFamilyMember> = emptyList(),
    val cefr: CefrLevel? = null,
)

/**
 * One usage example of a sense. A sense carries a list of these (one per usage
 * variant), never a single merged sentence (ADR-001). The [sentence] is
 * structured so the studied unit is explicitly marked — practice highlights /
 * cloze-masks it without fragile substring matching. [translation] is the
 * optional native-language rendering; [alignment] carries optional contiguous
 * (source, target) chunks for an aligned-segment UI. Both default to "absent".
 */
public data class ContextualApplication(
    val sentence: StudiedSentence,
    val translation: String? = null,
    val alignment: List<AlignmentChunk> = emptyList(),
)

/**
 * A neutral (source, target) chunk pair within a [ContextualApplication]:
 * a study-language span and its native-language counterpart, aligned as one
 * unit. Kept thin so the AI boundary mapper translates into it without leaking
 * the AI-core class into the lexical domain (ADR-005). Intentional boundary
 * mirror of the ai-core neutral / wire / persist `AlignmentChunk` reps — the
 * duplication keeps the lexical domain dependency-free, not a missing dedup.
 */
public data class AlignmentChunk(
    val source: String,
    val target: String,
)

/**
 * A derivative of a sense's base lemma — [lemma] with its part of speech
 * [unitType]. The list on a [Sense] is the lemma→derivative relationship the
 * app surfaces (e.g. `decide` → `decision`/noun, `decisive`/adjective).
 */
public data class WordFamilyMember(
    val lemma: String,
    val unitType: GrammarUnitType? = null,
)
