package app.sensee.feature.library.data

import app.sensee.feature.library.domain.Card
import app.sensee.feature.library.domain.CardId
import app.sensee.feature.library.domain.CardSummary
import app.sensee.feature.library.domain.CatalogOrigin
import app.sensee.feature.library.domain.Deck
import app.sensee.feature.library.domain.DeckId
import app.sensee.feature.library.domain.DeckWithCards
import app.sensee.feature.library.domain.Lemma
import app.sensee.feature.library.domain.LemmaDerivative
import app.sensee.feature.library.domain.LemmaId
import app.sensee.feature.library.domain.derivativesOfSenses
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.lexicon.domain.EntryStatus
import app.sensee.lexicon.domain.LexicalEntry
import app.sensee.lexicon.domain.Sense
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.engine.factory.SrsCardFactory

/**
 * Derives a read-only catalog deck from confirmed capture material (ADR-001:
 * Library is downstream of capture and adopts it).
 *
 * Family grouping: senses sharing a [Sense.headLemma] (falling back to
 * [Sense.baseLemma] when no head lemma was resolved, then to the entry's term)
 * share one parent [LemmaId] across entries, so `come`, `come across` and
 * `come up` pivot to each other under lemma `come`.
 *
 * Irregular form drill: any sense carrying [Sense.irregularForms] yields
 * lemma-stable form-variant cards for the Past and Past Participle (the
 * Infinitive is already drilled by the sense card itself). The trigger is the
 * presence of the principal parts, not [GrammarUnitType.IrregularVerb] — the AI
 * can classify a verb as plain `verb` and still ship the triplet, and the user
 * deserves the drill regardless of the POS label. Form-card ids are derived
 * from the parent lemma id + form slot, so re-capturing the same verb keeps the
 * existing SRS state and does not duplicate the cards across senses.
 *
 * Pure and storage-free — a fresh New SRS snapshot per read (same deferral as
 * capture persistence).
 */
internal object CapturedCatalogDerivation {
    val DECK_ID: DeckId = DeckId("captured")
    private const val ID_PREFIX = "captured:"
    private const val LEMMA_PREFIX = "captured-lemma:"
    private const val FORM_CARD_PREFIX = "captured:lemma:"
    private const val FORM_SUFFIX = ":form:"

    fun isCapturedCard(cardId: CardId): Boolean = cardId.value.startsWith(ID_PREFIX)

    fun isCapturedLemma(lemmaId: LemmaId): Boolean = lemmaId.value.startsWith(LEMMA_PREFIX)

    fun capturedDeck(entries: List<LexicalEntry>): DeckWithCards? {
        val cards = confirmedCards(entries)
        if (cards.isEmpty()) return null
        return DeckWithCards(
            deck =
                Deck(
                    id = DECK_ID,
                    title = "Сохранённые слова",
                    description = "Слова, сохранённые через быстрый захват",
                    cardCount = cards.size,
                    origin = CatalogOrigin.Personal,
                ),
            cards = cards,
        )
    }

    fun capturedLemma(
        entries: List<LexicalEntry>,
        lemmaId: LemmaId,
    ): Lemma? {
        val family = confirmedCards(entries).filter { it.lemmaId == lemmaId }
        if (family.isEmpty()) return null
        return Lemma(
            id = lemmaId,
            text = lemmaId.value.removePrefix(LEMMA_PREFIX),
            relatedCards =
                family.map {
                    CardSummary(
                        id = it.id,
                        headword = it.headword,
                        unitType = it.unitType,
                        translation = it.translation,
                        senseSummary = it.senseSummary,
                    )
                },
            derivatives = derivativesFor(entries, lemmaId),
        )
    }

    private fun confirmedCards(entries: List<LexicalEntry>): List<Card> {
        val all =
            entries
                .filter { it.status == EntryStatus.Confirmed }
                .flatMap { entry ->
                    entry.senses.flatMapIndexed { index, sense -> cardsFor(entry, index, sense) }
                }
        // Form cards are lemma-stable, so two senses of the same verb produce
        // the same form-card id. Keep the first occurrence; duplicates would
        // collide on SRS state and confuse the practice deck.
        val seen = mutableSetOf<CardId>()
        return all.filter { seen.add(it.id) }
    }

    private fun cardsFor(
        entry: LexicalEntry,
        index: Int,
        sense: Sense,
    ): List<Card> {
        val lemmaKey = familyLemmaKey(entry, sense)
        val lemmaId = LemmaId(LEMMA_PREFIX + lemmaKey)
        val baseCardId = "$ID_PREFIX${entry.id.value}:$index"
        val unitType = sense.unitType ?: GrammarUnitType.Phrase
        // Marked transport ([[target]]); practice parses it structurally for cloze.
        val example =
            sense.contextualApplications
                .firstOrNull()
                ?.sentence
                ?.marked()
                .orEmpty()
        val senseCard =
            Card(
                id = CardId(baseCardId),
                lemmaId = lemmaId,
                headword = sense.surfaceForm?.display() ?: entry.term,
                translation = sense.translation,
                contextSentence = example,
                unitType = unitType,
                grammarTags = sense.grammarTags,
                components = sense.components,
                senseSummary = sense.explanation ?: sense.translation,
                explanation = sense.explanation.orEmpty(),
                sense = sense,
                srs = SrsCardFactory.newCard(SrsCardId(baseCardId)),
            )
        val forms = sense.irregularForms ?: return listOf(senseCard)
        return listOf(senseCard) + irregularFormCards(lemmaKey, lemmaId, sense, forms, example)
    }

    private fun familyLemmaKey(
        entry: LexicalEntry,
        sense: Sense,
    ): String =
        sequenceOf(sense.headLemma, sense.baseLemma, entry.term)
            .filterNotNull()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?.lowercase()
            ?: entry.term.trim().lowercase()

    private fun lemmaIdFor(
        entry: LexicalEntry,
        sense: Sense,
    ): LemmaId = LemmaId(LEMMA_PREFIX + familyLemmaKey(entry, sense))

    // The lemma page's word family: derivatives every confirmed sense in this
    // family declared, deduped by lemma. Surfaces the lemma→derivative link
    // without merging the derivatives' own captured entries into this family.
    // Delegates the flatten/dedup loop to the shared helper so the captured
    // and catalog read paths produce a byte-identical list (I7).
    private fun derivativesFor(
        entries: List<LexicalEntry>,
        lemmaId: LemmaId,
    ): List<LemmaDerivative> =
        derivativesOfSenses(
            entries
                .asSequence()
                .filter { it.status == EntryStatus.Confirmed }
                .flatMap { entry -> entry.senses.asSequence().filter { lemmaIdFor(entry, it) == lemmaId } },
        )

    private fun irregularFormCards(
        lemmaKey: String,
        lemmaId: LemmaId,
        sense: Sense,
        forms: IrregularForms,
        example: String,
    ): List<Card> =
        listOf(
            GrammarForm.PastTense to forms.past,
            GrammarForm.PastParticiple to forms.pastParticiple,
        ).map { (form, word) ->
            // Lemma-stable id: re-capturing the same verb (or another sense of
            // it) reuses the existing SRS row instead of forking a duplicate.
            val variantId = "$FORM_CARD_PREFIX$lemmaKey$FORM_SUFFIX${form.id}"
            Card(
                id = CardId(variantId),
                lemmaId = lemmaId,
                headword = word,
                translation = sense.translation,
                contextSentence = example,
                unitType = GrammarUnitType.IrregularVerb,
                grammarTags = listOf(GrammarTag(GrammarCategory.VerbIrregular, form)),
                senseSummary = "${forms.base} / ${forms.past} / ${forms.pastParticiple}",
                explanation = sense.explanation.orEmpty(),
                sense = sense,
                srs = SrsCardFactory.newCard(SrsCardId(variantId)),
            )
        }
}
