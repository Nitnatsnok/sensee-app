package app.sensee.feature.library.data

import app.sensee.feature.library.data.local.CatalogLocalDataSource
import app.sensee.feature.library.data.remote.CatalogRemoteDataSource
import app.sensee.feature.library.domain.Card
import app.sensee.feature.library.domain.CardId
import app.sensee.feature.library.domain.CatalogRepository
import app.sensee.feature.library.domain.Deck
import app.sensee.feature.library.domain.DeckId
import app.sensee.feature.library.domain.DeckWithCards
import app.sensee.feature.library.domain.Lemma
import app.sensee.feature.library.domain.LemmaId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow

/**
 * The bound catalog. Decks come from the local deck structure synced from the
 * remote source; deck cards and the captured deck are projected from the canonical
 * sense store (ADR-001), so Capture -> Library -> Practice closes on one store.
 * Review writes are a separate practice-owned concern
 * ([app.sensee.feature.practice.domain.PracticeReviewRepository]).
 */
@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<CatalogRepository>(),
)
@Inject
public class DefaultCatalogRepository(
    private val localDataSource: CatalogLocalDataSource,
    private val remoteDataSource: CatalogRemoteDataSource,
) : CatalogRepository {
    override fun observeOwnedMaterial(): Flow<List<Deck>> = localDataSource.observeOwnedMaterial()

    public fun observeAllDecks(): Flow<List<Deck>> = localDataSource.observeAllDecks()

    override suspend fun refreshFromRemote() {
        localDataSource.upsertDeckSummaries(remoteDataSource.listDecks().decks)
    }

    override suspend fun loadDeck(deckId: DeckId): DeckWithCards {
        if (deckId == SenseCatalogProjection.CAPTURED_DECK_ID) {
            return requireNotNull(localDataSource.capturedDeck()) {
                "Captured deck requested but no confirmed senses exist"
            }
        }
        localDataSource.ingestDeck(remoteDataSource.getDeck(deckId.value))
        return requireNotNull(localDataSource.selectDeckWithCards(deckId.value)) {
            "Deck $deckId disappeared after sync"
        }
    }

    /**
     * Subscribe to a Service deck: fetch it from remote (outside any transaction),
     * then ingest its senses/SRS/membership and flip the subscribed flag in ONE
     * transaction, so adopting commits atomically.
     */
    internal suspend fun subscribeDeck(deckId: DeckId) {
        localDataSource.ingestDeck(remoteDataSource.getDeck(deckId.value), subscribe = true)
    }

    override suspend fun loadCard(cardId: CardId): Card =
        requireNotNull(localDataSource.selectCard(cardId.value)) {
            "Card $cardId not found in local store"
        }

    override suspend fun loadLemma(lemmaId: LemmaId): Lemma =
        requireNotNull(localDataSource.selectLemma(lemmaId.value)) {
            "Lemma $lemmaId has no confirmed senses"
        }
}
