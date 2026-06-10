package app.sensee.feature.library.data

import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.feature.library.data.local.CatalogLocalDataSource
import app.sensee.feature.library.data.remote.CatalogRemoteDataSource
import app.sensee.feature.library.domain.Card
import app.sensee.feature.library.domain.CardId
import app.sensee.feature.library.domain.CatalogOrigin
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * The bound catalog. Decks come from the local deck structure synced from the
 * remote source; deck cards and the captured deck are projected from the canonical
 * sense store (ADR-001), so Capture -> Library -> Practice closes on one store.
 * Remote writes happen only in explicit sync/adopt flows; reading a deck is a
 * local projection. Review writes are a separate practice-owned concern
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
    appDiagnostics: AppDiagnostics,
) : CatalogRepository {
    private val logger = appDiagnostics.logger.tag("CatalogSync")

    override fun observeOwnedMaterial(): Flow<List<Deck>> = localDataSource.observeOwnedMaterial()

    public fun observeAllDecks(): Flow<List<Deck>> = localDataSource.observeAllDecks()

    override suspend fun refreshFromRemote() {
        val summaries = remoteDataSource.listDecks().decks
        localDataSource.upsertDeckSummaries(summaries)
        syncSubscribedDeckContent(summaries.mapTo(mutableSetOf()) { it.id })
    }

    override suspend fun loadDeck(deckId: DeckId): DeckWithCards {
        if (deckId == SenseCatalogProjection.CAPTURED_DECK_ID) {
            return requireNotNull(localDataSource.capturedDeck()) {
                "Captured deck requested but no confirmed senses exist"
            }
        }
        return requireNotNull(localDataSource.selectDeckWithCards(deckId.value)) {
            "Deck $deckId not found in local catalog cache"
        }
    }

    override suspend fun previewDeck(deckId: DeckId): DeckWithCards {
        if (deckId == SenseCatalogProjection.CAPTURED_DECK_ID) {
            return requireNotNull(localDataSource.capturedDeck()) {
                "Captured deck requested but no confirmed senses exist"
            }
        }
        // A subscribed deck (CatalogOrigin.Personal) is already synced locally, so read it there.
        // A passive Service suggestion has a local meta row but no membership, so a local read
        // would be empty — project it from the remote source instead, in memory, never ingested.
        // Adopting the deck (subscribe) is the only thing that writes.
        val local = localDataSource.selectDeckWithCards(deckId.value)
        return if (local != null && local.deck.origin == CatalogOrigin.Personal) {
            local
        } else {
            remoteDataSource.getDeck(deckId.value).toPreviewDeckWithCards()
        }
    }

    private suspend fun syncSubscribedDeckContent(remoteDeckIds: Set<String>) {
        val deckIds =
            localDataSource
                .selectSubscribedDecks()
                .map { it.id.value }
                .filter { it in remoteDeckIds }
        if (deckIds.isEmpty()) return
        // Fetch every subscribed deck concurrently (the slow, independent part) and
        // isolate per-deck failures: one deck that fails to load is skipped (and
        // retried next refresh) instead of aborting the others or the whole refresh.
        // Ingest stays sequential — the local store is a single writer, and a
        // re-sync overwrites mirrored content without materializing any SRS state.
        val fetched =
            coroutineScope {
                deckIds
                    .map { deckId ->
                        async {
                            runCatchingCancellable { remoteDataSource.getDeck(deckId) }
                                .onFailure { logger.warn(it) { "Subscribed deck $deckId content sync skipped" } }
                                .getOrNull()
                        }
                    }.awaitAll()
            }
        fetched.filterNotNull().forEach { localDataSource.ingestDeck(it) }
    }

    /**
     * Subscribe to a Service deck: fetch it from remote (outside any transaction),
     * then ingest its senses/membership and flip the subscribed flag in ONE
     * transaction, so adopting commits atomically. Practice creates SRS state when
     * a card is actually reviewed.
     */
    internal suspend fun subscribeDeck(deckId: DeckId) {
        localDataSource.ingestDeck(remoteDataSource.getDeck(deckId.value), subscribe = true)
    }

    override suspend fun loadCard(cardId: CardId): Card =
        requireNotNull(localDataSource.selectCards(listOf(cardId.value)).firstOrNull()) {
            "Card $cardId not found in local store"
        }

    override suspend fun loadCards(cardIds: List<CardId>): List<Card> =
        localDataSource.selectCards(cardIds.map { it.value })

    override suspend fun loadLemma(lemmaId: LemmaId): Lemma =
        requireNotNull(localDataSource.selectLemma(lemmaId.value)) {
            "Lemma $lemmaId has no confirmed senses"
        }
}
