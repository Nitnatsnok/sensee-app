package app.sensee.feature.library.domain

import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.srs.core.model.SrsCardSnapshot

/**
 * A practice card carries a single meaning of a [headword].
 *
 * Meanings are segregated: "come up" and "come up with" each own their own [Card] because the
 * preposition changes the meaning. Cards that share a [lemmaId] are siblings under the same lemma
 * (e.g. all derivatives of "come"); navigating to a sibling card is how a learner explores the
 * lemma family.
 */
public data class Card(
    val id: CardId,
    val lemmaId: LemmaId,
    val headword: String,
    val translation: String,
    val contextSentence: String,
    val unitType: GrammarUnitType,
    val grammarTags: List<GrammarTag>,
    val senseSummary: String,
    val explanation: String,
    val srs: SrsCardSnapshot,
)
