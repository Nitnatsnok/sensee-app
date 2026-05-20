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
 * Adoption marks a synced Service deck as owned ([CatalogLocalDataSource.markDeckAdopted]):
 * the content is already in the local catalog tables, so this is a provenance flip,
 * not a copy. Shared cards stay a single [app.sensee.core.database.Practice_card] row
 * (the deck<->card join keeps the membership), so adopting two decks that share a card
 * never duplicates it. The derived captured deck has no DB row and is always Personal,
 * so it is neither adoptable nor un-adoptable.
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
        require(deckId != CapturedCatalogDerivation.DECK_ID) {
            "The captured deck is already owned and cannot be adopted"
        }
        base.loadDeck(deckId)
        localDataSource.markDeckAdopted(deckId.value)
    }

    override suspend fun unAdopt(deckId: DeckId) {
        require(deckId != CapturedCatalogDerivation.DECK_ID) {
            "The captured deck is derived from capture and cannot be un-adopted"
        }
        localDataSource.clearDeckAdopted(deckId.value)
    }

    override fun observeAdoptedDecks(): Flow<List<Deck>> = localDataSource.observeOwnedDecks()

    override fun observeSuggestedDecks(): Flow<List<Deck>> =
        base.observeAllDecks().map { decks -> decks.filter { it.origin == CatalogOrigin.Service } }
}
