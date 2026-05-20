package app.sensee.feature.library.data

import app.sensee.feature.library.domain.Card
import app.sensee.feature.library.domain.CardId
import app.sensee.feature.library.domain.CardSummary
import app.sensee.feature.library.domain.CatalogOrigin
import app.sensee.feature.library.domain.Deck
import app.sensee.feature.library.domain.DeckId
import app.sensee.feature.library.domain.DeckWithCards
import app.sensee.feature.library.domain.Lemma
import app.sensee.feature.library.domain.LemmaId
import app.sensee.feature.vocabularyEditor.domain.EntryStatus
import app.sensee.feature.vocabularyEditor.domain.LexicalEntry
import app.sensee.feature.vocabularyEditor.domain.Meaning
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.engine.factory.SrsCardFactory

/**
 * Derives a read-only catalog deck from confirmed capture material (ADR-001:
 * Library is downstream of capture and adopts it). Senses with the same base
 * lemma share one parent [LemmaId] across entries, so any captured card can
 * pivot to its siblings (come / come up / come across). An irregular-verb
 * sense additionally yields related form-variant cards (come / came / come)
 * tagged structurally via [GrammarCategory.VerbIrregular], never free text.
 * Pure and storage-free — a fresh New SRS snapshot per read (same deferral as
 * capture persistence).
 */
internal object CapturedCatalogDerivation {
    val DECK_ID: DeckId = DeckId("captured")
    private const val ID_PREFIX = "captured:"
    private const val LEMMA_PREFIX = "captured-lemma:"

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
        )
    }

    private fun confirmedCards(entries: List<LexicalEntry>): List<Card> =
        entries
            .filter { it.status == EntryStatus.Confirmed }
            .flatMap { entry -> entry.meanings.flatMapIndexed { index, meaning -> cardsFor(entry, index, meaning) } }

    private fun cardsFor(
        entry: LexicalEntry,
        index: Int,
        meaning: Meaning,
    ): List<Card> {
        val lemmaKey = (meaning.baseLemma ?: entry.term).trim().lowercase()
        val lemmaId = LemmaId(LEMMA_PREFIX + lemmaKey)
        val baseCardId = "$ID_PREFIX${entry.id.value}:$index"
        val unitType = meaning.unitType ?: GrammarUnitType.Phrase
        // Marked transport ([[target]]); practice parses it structurally for cloze.
        val example =
            meaning.contextualApplications
                .firstOrNull()
                ?.sentence
                ?.marked()
                .orEmpty()
        val senseCard =
            Card(
                id = CardId(baseCardId),
                lemmaId = lemmaId,
                headword = meaning.surfaceForm?.display() ?: entry.term,
                translation = meaning.translation,
                contextSentence = example,
                unitType = unitType,
                grammarTags = meaning.grammarTags,
                senseSummary = meaning.explanation ?: meaning.translation,
                explanation = meaning.explanation.orEmpty(),
                srs = SrsCardFactory.newCard(SrsCardId(baseCardId)),
            )
        val forms = meaning.irregularForms
        if (unitType != GrammarUnitType.IrregularVerb || forms == null) {
            return listOf(senseCard)
        }
        val variants = irregularVerbVariantCards(baseCardId, lemmaId, meaning, example)
        return listOf(senseCard) + variants
    }

    private fun irregularVerbVariantCards(
        baseCardId: String,
        lemmaId: LemmaId,
        meaning: Meaning,
        example: String,
    ): List<Card> {
        val forms = meaning.irregularForms ?: return emptyList()
        return listOf(
            GrammarForm.Infinitive to forms.base,
            GrammarForm.PastTense to forms.past,
            GrammarForm.PastParticiple to forms.pastParticiple,
        ).map { (form, word) ->
            val variantId = "$baseCardId:form:${form.id}"
            Card(
                id = CardId(variantId),
                lemmaId = lemmaId,
                headword = word,
                translation = meaning.translation,
                contextSentence = example,
                unitType = GrammarUnitType.IrregularVerb,
                grammarTags = listOf(GrammarTag(GrammarCategory.VerbIrregular, form)),
                senseSummary = "${forms.base} / ${forms.past} / ${forms.pastParticiple}",
                explanation = meaning.explanation.orEmpty(),
                srs = SrsCardFactory.newCard(SrsCardId(variantId)),
            )
        }
    }
}
