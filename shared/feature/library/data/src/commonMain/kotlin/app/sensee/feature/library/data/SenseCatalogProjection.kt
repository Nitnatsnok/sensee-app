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
import app.sensee.feature.library.domain.derivativesOfSenses
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.lexicon.domain.SenseId
import app.sensee.lexicon.domain.StoredSense
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.engine.factory.SrsCardFactory

/**
 * Projects canonical [StoredSense]s into the catalog's read models — practice
 * [Card]s and [Lemma] pages. A card id IS the stable `sense_id` (so SRS state
 * survives any content edit). The id scheme reserves a `${sense_id}:form:${slot}`
 * shape for per-form cards (parseable because a [SenseId] never contains `:`), but
 * those are not auto-generated — irregular-verb form cards become an explicit user
 * choice at capture time.
 *
 * Family grouping is by [StoredSense.lemmaKey] (the head-lemma key the store
 * already derived), so `come`, `come across` and `come up` share one [LemmaId].
 * A fresh New SRS snapshot rides each projected card; the repository overlays the
 * stored snapshot on read.
 */
internal object SenseCatalogProjection {
    val CAPTURED_DECK_ID: DeckId = DeckId("captured")
    private const val LEMMA_PREFIX = "lemma:"
    private const val FORM_INFIX = ":form:"

    fun lemmaId(lemmaKey: String): LemmaId = LemmaId(LEMMA_PREFIX + lemmaKey)

    fun lemmaKeyOf(lemmaId: LemmaId): String = lemmaId.value.removePrefix(LEMMA_PREFIX)

    /** The owning sense_id of a card id, stripping a trailing form slot. */
    fun senseIdOf(cardId: CardId): SenseId = SenseId(cardId.value.substringBefore(FORM_INFIX))

    fun capturedDeck(senses: List<StoredSense>): DeckWithCards? {
        val cards = cardsFrom(senses)
        if (cards.isEmpty()) return null
        return DeckWithCards(
            deck =
                Deck(
                    id = CAPTURED_DECK_ID,
                    title = "Сохранённые слова",
                    description = "Слова, сохранённые через быстрый захват",
                    cardCount = cards.size,
                    origin = CatalogOrigin.Personal,
                ),
            cards = cards,
        )
    }

    fun lemma(
        senses: List<StoredSense>,
        lemmaId: LemmaId,
    ): Lemma? {
        val family = senses.filter { lemmaId(it.lemmaKey) == lemmaId }
        if (family.isEmpty()) return null
        return Lemma(
            id = lemmaId,
            text = lemmaKeyOf(lemmaId),
            relatedCards =
                cardsFrom(family).map {
                    CardSummary(
                        id = it.id,
                        headword = it.headword,
                        unitType = it.unitType,
                        translation = it.translation,
                        senseSummary = it.senseSummary,
                    )
                },
            derivatives = derivativesOfSenses(family.asSequence().map { it.sense }),
        )
    }

    /** Project each sense and dedup by card id (a sense listed twice cannot duplicate). */
    fun cardsFrom(senses: List<StoredSense>): List<Card> {
        val seen = mutableSetOf<CardId>()
        return senses.flatMap { cardsFor(it) }.filter { seen.add(it.id) }
    }

    fun cardsFor(stored: StoredSense): List<Card> {
        val sense = stored.sense
        val lemmaId = lemmaId(stored.lemmaKey)
        // Marked transport ([[target]]); practice parses it structurally for cloze.
        val example =
            sense.contextualApplications
                .firstOrNull()
                ?.sentence
                ?.marked()
                .orEmpty()
        val senseCard =
            Card(
                id = CardId(stored.id.value),
                lemmaId = lemmaId,
                headword = sense.surfaceForm?.display() ?: sense.translation,
                translation = sense.translation,
                contextSentence = example,
                unitType = sense.unitType ?: GrammarUnitType.Phrase,
                grammarTags = sense.grammarTags,
                components = sense.components,
                senseSummary = sense.explanation ?: sense.translation,
                explanation = sense.explanation.orEmpty(),
                sense = sense,
                srs = SrsCardFactory.newCard(SrsCardId(stored.id.value)),
            )
        // One card per sense. Irregular-verb form cards (took/taken) are no longer
        // auto-generated from `irregular_forms`; they will become an explicit user
        // choice at capture time. The `:form:` id scheme below stays reserved for them.
        return listOf(senseCard)
    }
}
