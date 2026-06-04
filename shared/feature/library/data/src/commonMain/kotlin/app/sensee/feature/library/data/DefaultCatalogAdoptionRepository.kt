package app.sensee.feature.library.data

import app.sensee.feature.library.data.local.CatalogLocalDataSource
import app.sensee.feature.library.domain.CatalogAdoptionRepository
import app.sensee.feature.library.domain.CatalogOrigin
import app.sensee.feature.library.domain.Deck
import app.sensee.feature.library.domain.DeckId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Subscribing mirrors a Service deck into the user's practiced material: ingest
 * the deck's senses (origin = Service, keyed by source_ref) and flip the deck's
 * `subscribed` flag. It is not a copy — a re-sync overwrites mirrored content and
 * SRS state on the shared sense_id stays intact. The derived captured deck has no
 * deck row and is always owned, so it is neither subscribable nor unsubscribable.
 */
@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<CatalogAdoptionRepository>(),
)
@Inject
public class DefaultCatalogAdoptionRepository(
    private val base: DefaultCatalogRepository,
    private val localDataSource: CatalogLocalDataSource,
) : CatalogAdoptionRepository {
    override suspend fun adopt(deckId: DeckId) {
        require(deckId != SenseCatalogProjection.CAPTURED_DECK_ID) {
            "The captured deck is already owned and cannot be subscribed"
        }
        base.loadDeck(deckId)
        localDataSource.setDeckSubscribed(deckId.value, subscribed = true)
    }

    override suspend fun unAdopt(deckId: DeckId) {
        require(deckId != SenseCatalogProjection.CAPTURED_DECK_ID) {
            "The captured deck is derived from capture and cannot be unsubscribed"
        }
        localDataSource.setDeckSubscribed(deckId.value, subscribed = false)
    }

    override fun observeAdoptedDecks(): Flow<List<Deck>> = localDataSource.observeSubscribedDecks()

    override fun observeSuggestedDecks(): Flow<List<Deck>> =
        base.observeAllDecks().map { decks -> decks.filter { it.origin == CatalogOrigin.Service } }
}
