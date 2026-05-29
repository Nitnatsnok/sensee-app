package app.sensee.feature.library.domain

import app.sensee.grammar.domain.GrammarUnitType

/**
 * A lemma groups cards that share a base lexical form.
 *
 * For example the lemma "come" is the parent of "come", "come up", "come up with", "come across".
 * A learner viewing any card in the family can pivot to siblings through [relatedCards].
 */
public data class Lemma(
    val id: LemmaId,
    val text: String,
    val relatedCards: List<CardSummary>,
    val derivatives: List<LemmaDerivative> = emptyList(),
)

/**
 * A derivative in a lemma's word family — [text] with its part of speech
 * [unitType]. Surfaces the lemma→derivative relationship on the lemma page
 * (e.g. `decide` → `decision`/noun, `decisive`/adjective).
 */
public data class LemmaDerivative(
    val text: String,
    val unitType: GrammarUnitType? = null,
)

/**
 * A lightweight projection of a [Card] used for navigation lists where the full SRS payload is not
 * needed.
 */
public data class CardSummary(
    val id: CardId,
    val headword: String,
    val unitType: GrammarUnitType,
    val translation: String,
    val senseSummary: String,
)
