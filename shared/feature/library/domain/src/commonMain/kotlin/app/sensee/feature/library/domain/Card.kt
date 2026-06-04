package app.sensee.feature.library.domain

import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.UnitComponent
import app.sensee.lexicon.domain.Sense
import app.sensee.srs.core.model.SrsCardSnapshot

/**
 * A practice card carries a single sense of a [headword].
 *
 * Senses are segregated: "come up" and "come up with" each own their own [Card] because the
 * preposition changes the sense. Cards that share a [lemmaId] are siblings under the same lemma
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
    val components: List<UnitComponent> = emptyList(),
    val senseSummary: String,
    val explanation: String,
    // Full rich sense the card was built from (service deck or capture). The
    // lean fields above stay the flashcard essentials; [sense] carries the
    // rich detail (examples, synonyms, word family, …) for the card/lemma
    // detail view.
    val sense: Sense? = null,
    val srs: SrsCardSnapshot,
)
