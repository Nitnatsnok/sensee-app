package app.sensee.feature.library.data

import app.sensee.feature.library.domain.Card
import app.sensee.feature.library.domain.CardId
import app.sensee.feature.library.domain.CatalogRepository
import app.sensee.feature.library.domain.Deck
import app.sensee.feature.library.domain.DeckId
import app.sensee.feature.library.domain.DeckWithCards
import app.sensee.feature.library.domain.Lemma
import app.sensee.feature.library.domain.LemmaId
import app.sensee.feature.vocabularyEditor.domain.VocabularyRepository
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.engine.storage.SrsStorage
import app.sensee.srs.fsrs.FsrsParameters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * The bound catalog. Library's catalog is the synced material ([base]) plus a
 * read-only deck derived from confirmed capture entries — this is the seam that
 * closes Capture -> Library -> Practice: a confirmed word appears as a card in
 * the existing Practice surface with no separate screen needed.
 *
 * Dependency direction stays correct (ADR-001): library/data depends on
 * vocabulary-editor/domain, never the reverse.
 */
@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<CatalogRepository>(),
)
@Inject
public class CapturedCatalogRepository(
    private val base: DefaultCatalogRepository,
    private val vocabulary: VocabularyRepository,
    private val srsStorage: SrsStorage<FsrsParameters>,
) : CatalogRepository {
    // Captured cards are re-derived each read (ADR-001 projection), but their
    // review state is durable: materialize the first SRS row before Practice
    // can submit a review, then overlay stored progress on later reads.
    private suspend fun withMaterializedSrs(card: Card): Card {
        val cardId = SrsCardId(card.id.value)
        val stored = srsStorage.getCard(cardId)
        if (stored != null) {
            return card.copy(srs = stored)
        }
        return card.copy(srs = srsStorage.saveCardIfAbsent(card.srs))
    }

    // Adopted material + the derived captured deck. Reactive: emissions fire when either
    // the local catalog (adoption flips) or the captured projection (a new confirmed entry)
    // change, so the Library/Practice UI updates without manual reload.
    override fun observeOwnedMaterial(): Flow<List<Deck>> =
        combine(base.observeOwnedMaterial(), vocabulary.observeEntries()) { owned, entries ->
            val captured = CapturedCatalogDerivation.capturedDeck(entries)
            if (captured == null) owned else owned + captured.deck
        }

    override suspend fun refreshFromRemote() {
        base.refreshFromRemote()
    }

    override suspend fun loadDeck(deckId: DeckId): DeckWithCards {
        if (deckId == CapturedCatalogDerivation.DECK_ID) {
            val deck =
                requireNotNull(CapturedCatalogDerivation.capturedDeck(vocabulary.listEntries())) {
                    "Captured deck requested but no confirmed entries exist"
                }
            return deck.copy(cards = deck.cards.map { withMaterializedSrs(it) })
        }
        return base.loadDeck(deckId)
    }

    override suspend fun loadCard(cardId: CardId): Card {
        if (CapturedCatalogDerivation.isCapturedCard(cardId)) {
            val cards = CapturedCatalogDerivation.capturedDeck(vocabulary.listEntries())?.cards.orEmpty()
            val card =
                requireNotNull(cards.firstOrNull { it.id == cardId }) {
                    "Captured card $cardId not found"
                }
            return withMaterializedSrs(card)
        }
        return base.loadCard(cardId)
    }

    override suspend fun loadLemma(lemmaId: LemmaId): Lemma {
        if (CapturedCatalogDerivation.isCapturedLemma(lemmaId)) {
            return requireNotNull(
                CapturedCatalogDerivation.capturedLemma(vocabulary.listEntries(), lemmaId),
            ) { "Captured lemma $lemmaId requested but no confirmed senses exist" }
        }
        return base.loadLemma(lemmaId)
    }
}
