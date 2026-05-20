package app.sensee.feature.library.data

import app.sensee.feature.library.data.local.CatalogLocalDataSource
import app.sensee.feature.library.data.local.toLemma
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
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow

/**
 * Library-owned implementation of the synced catalog (remote -> local SQLDelight
 * projection). It is not the bound [CatalogRepository]: [CapturedCatalogRepository]
 * decorates it to also surface confirmed capture material (ADR-001). Review writes
 * are a separate practice-owned concern
 * ([app.sensee.feature.practice.domain.PracticeReviewRepository]).
 */
@SingleIn(AppScope::class)
@Inject
public class DefaultCatalogRepository(
    private val localDataSource: CatalogLocalDataSource,
    private val remoteDataSource: CatalogRemoteDataSource,
) : CatalogRepository {
    override fun observeOwnedMaterial(): Flow<List<Deck>> = localDataSource.observeOwnedDecks()

    public fun observeAllDecks(): Flow<List<Deck>> = localDataSource.observeAllDecks()

    override suspend fun refreshFromRemote() {
        val remote = remoteDataSource.listDecks()
        localDataSource.upsertDeckSummaries(remote.decks)
    }

    override suspend fun loadDeck(deckId: DeckId): DeckWithCards {
        val remote = remoteDataSource.getDeck(deckId.value)
        localDataSource.upsertDeckWithCards(remote)
        return requireNotNull(localDataSource.selectDeckWithCards(deckId.value)) {
            "Deck $deckId disappeared after sync"
        }
    }

    override suspend fun loadCard(cardId: CardId): Card =
        requireNotNull(localDataSource.selectCard(cardId.value)) {
            "Card $cardId not found in local store"
        }

    override suspend fun loadLemma(lemmaId: LemmaId): Lemma {
        val remote = remoteDataSource.getLemma(lemmaId.value)
        localDataSource.upsertLemma(remote)
        return localDataSource.selectLemma(lemmaId.value) ?: remote.toLemma()
    }
}
