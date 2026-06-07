package app.sensee.feature.library.domain

import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.lexicon.domain.Sense

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
 * Flattens `wordFamily` across [senses] into a deduplicated [LemmaDerivative]
 * list. Trim + case-fold for dedup, drop blanks. Single source of truth: the
 * catalog lemma projection calls this for both the captured (Personal) and the
 * subscribed (Service) lemma pages, so the word family stays identical no matter
 * which senses produce it.
 */
public fun derivativesOfSenses(senses: Sequence<Sense>): List<LemmaDerivative> {
    val seen = mutableSetOf<String>()
    return senses
        .flatMap { it.wordFamily.asSequence() }
        .mapNotNull { member ->
            val text = member.lemma.trim()
            if (text.isEmpty() || !seen.add(text.lowercase())) null else LemmaDerivative(text, member.unitType)
        }.toList()
}

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
