package app.sensee.feature.library.data.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.database.CatalogEntityQueries
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.logging.AppLogger
import app.sensee.database.DatabaseTransactionRunner
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.library.data.SenseCatalogProjection
import app.sensee.feature.library.data.remote.DeckDto
import app.sensee.feature.library.data.remote.DeckSummaryDto
import app.sensee.feature.library.domain.Card
import app.sensee.feature.library.domain.CardId
import app.sensee.feature.library.domain.CatalogOrigin
import app.sensee.feature.library.domain.Deck
import app.sensee.feature.library.domain.DeckId
import app.sensee.feature.library.domain.DeckWithCards
import app.sensee.feature.library.domain.Lemma
import app.sensee.feature.library.domain.LemmaId
import app.sensee.lexicon.domain.SenseId
import app.sensee.lexicon.domain.SenseOrigin
import app.sensee.lexicon.domain.SenseReadRepository
import app.sensee.lexicon.domain.SenseStatus
import app.sensee.lexicon.domain.SenseWriteRepository
import app.sensee.lexicon.domain.StoredSense
import app.sensee.lexicon.domain.WriteIntent
import app.sensee.lexicon.domain.isConfirmable
import app.sensee.lexicon.serialization.toDomain
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.engine.factory.SrsCardFactory
import app.sensee.srs.engine.storage.SrsStorage
import app.sensee.srs.fsrs.FsrsParameters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Catalog-side local store. Deck structure (deck / deck_membership) is owned here;
 * sense content comes from the canonical [SenseReadRepository] and service ingest
 * writes through [SenseWriteRepository] (origin = Service, keyed by the catalog
 * card id as source_ref). The per-card SRS snapshot is fetched through the
 * [SrsStorage] contract (owned by Practice), keyed by the stable sense_id.
 */
@SingleIn(AppScope::class)
@Inject
public class CatalogLocalDataSource(
    private val databaseProvider: SenseeDatabaseProvider,
    private val transactionRunner: DatabaseTransactionRunner,
    private val senseReadRepository: SenseReadRepository,
    private val senseWriteRepository: SenseWriteRepository,
    private val srsStorage: SrsStorage<FsrsParameters>,
    private val dispatchers: AppDispatchers,
    appDiagnostics: AppDiagnostics,
) {
    private val logger: AppLogger = appDiagnostics.logger.tag("CatalogLocalDataSource")

    public suspend fun upsertDeckSummaries(decks: List<DeckSummaryDto>) {
        val database = databaseProvider.database()
        decks.forEach { deck ->
            database.catalogEntityQueries.upsertDeckMeta(deck.id, deck.title, deck.description, deck.cardCount.toLong())
        }
    }

    public suspend fun setDeckSubscribed(
        deckId: String,
        subscribed: Boolean,
    ) {
        databaseProvider.database().catalogEntityQueries.setDeckSubscribed(
            subscribed = if (subscribed) 1L else 0L,
            id = deckId,
        )
    }

    /**
     * Ingest a Service deck in ONE transaction: map each card's `SenseDto` into a
     * Sense, upsert it (origin = Service, source_ref = card id), materialize its SRS
     * seat, and rebuild the deck membership by sense_id — and, when [subscribe], flip
     * the deck's subscribed flag in the same transaction so an adopt commits all of
     * its durable state or none of it (no orphan sense/SRS rows, no half-subscribed
     * deck). A card whose sense fails the example/translation invariant is dropped
     * with a diagnostic rather than entering the deck as a broken card.
     */
    public suspend fun ingestDeck(
        deck: DeckDto,
        subscribe: Boolean = false,
    ) {
        val database = databaseProvider.database()
        transactionRunner.transaction {
            val memberships = mutableListOf<Pair<String, Int>>()
            deck.cards.forEach { card ->
                val sense = card.sense.toDomain()
                if (!sense.isConfirmable()) {
                    logger.warn {
                        "Service card ${card.id} dropped: no confirmable sense (needs a translation and an example)"
                    }
                    return@forEach
                }
                val stored =
                    senseWriteRepository.upsert(
                        sense = sense,
                        status = SenseStatus.Confirmed,
                        origin = SenseOrigin.Service,
                        sourceRef = card.id,
                        intent = WriteIntent.ResolveOrMint,
                    )
                srsStorage.saveCardIfAbsent(SrsCardFactory.newCard(SrsCardId(stored.id.value)))
                // Position off the kept list, not the source index: a dropped card
                // must not leave a gap in the deck's membership positions.
                memberships += stored.id.value to memberships.size
            }
            database.catalogEntityQueries.upsertDeckMeta(
                deck.id,
                deck.title,
                deck.description,
                deck.cards.size.toLong(),
            )
            database.catalogEntityQueries.clearDeckMembership(deck.id)
            memberships.forEach { (senseId, position) ->
                database.catalogEntityQueries.insertDeckMembership(
                    deck_id = deck.id,
                    sense_id = senseId,
                    position = position.toLong(),
                )
            }
            if (subscribe) {
                database.catalogEntityQueries.setDeckSubscribed(subscribed = 1L, id = deck.id)
            }
        }
    }

    public suspend fun selectDecks(): List<Deck> =
        databaseProvider
            .database()
            .catalogEntityQueries
            .selectAllDecks()
            .awaitAsList()
            .map { deck(it.id, it.title, it.description, it.subscribed, it.card_count) }

    public fun observeAllDecks(): Flow<List<Deck>> =
        flow {
            val database = databaseProvider.database()
            emitAll(
                database.catalogEntityQueries
                    .selectAllDecks()
                    .asFlow()
                    .mapToList(dispatchers.io)
                    .map { rows -> rows.map { deck(it.id, it.title, it.description, it.subscribed, it.card_count) } },
            )
        }

    public fun observeSubscribedDecks(): Flow<List<Deck>> =
        flow {
            val database = databaseProvider.database()
            emitAll(
                database.catalogEntityQueries
                    .selectSubscribedDecks()
                    .asFlow()
                    .mapToList(dispatchers.io)
                    .map { rows -> rows.map { deck(it.id, it.title, it.description, it.subscribed, it.card_count) } },
            )
        }

    /** Subscribed decks plus the derived captured deck (Personal Confirmed senses). */
    public fun observeOwnedMaterial(): Flow<List<Deck>> =
        combine(observeSubscribedDecks(), senseReadRepository.observe()) { subscribed, all ->
            val captured = SenseCatalogProjection.capturedDeck(all.personalConfirmed())
            if (captured == null) subscribed else subscribed + captured.deck
        }

    public suspend fun selectDeckWithCards(deckId: String): DeckWithCards? {
        val database = databaseProvider.database()
        val deckRow = database.catalogEntityQueries.selectDeckById(deckId).awaitAsOneOrNull() ?: return null
        // Bulk-load member senses in one query, then restore the membership order
        // (and drop any sense that is gone) — no per-card round-trip.
        val ids =
            database.catalogEntityQueries
                .selectMembershipByDeck(deckId)
                .awaitAsList()
                .map { SenseId(it.sense_id) }
        val byId = senseReadRepository.getByIds(ids).associateBy { it.id }
        val stored = ids.mapNotNull { byId[it] }
        return DeckWithCards(
            deck = deck(deckRow.id, deckRow.title, deckRow.description, deckRow.subscribed, deckRow.card_count),
            cards = materializeAll(SenseCatalogProjection.cardsFrom(stored)),
        )
    }

    public suspend fun capturedDeck(): DeckWithCards? {
        val deck =
            SenseCatalogProjection.capturedDeck(
                senseReadRepository.listByStatus(SenseStatus.Confirmed).personalConfirmed(),
            )
        return deck?.copy(cards = materializeAll(deck.cards))
    }

    public suspend fun selectCard(cardId: String): Card? {
        val stored = senseReadRepository.getById(SenseCatalogProjection.senseIdOf(CardId(cardId))) ?: return null
        val card = SenseCatalogProjection.cardsFor(stored).firstOrNull { it.id.value == cardId } ?: return null
        return materializeAll(listOf(card)).firstOrNull()
    }

    public suspend fun selectLemma(lemmaId: String): Lemma? {
        val key = SenseCatalogProjection.lemmaKeyOf(LemmaId(lemmaId))
        val stored = senseReadRepository.listByLemmaKey(key).filter { it.status == SenseStatus.Confirmed }
        return SenseCatalogProjection.lemma(stored, LemmaId(lemmaId))
    }

    // A projected card carries a fresh New SRS snapshot; overlay the durable one
    // when it exists. The durable seat MUST exist before Practice can review (the
    // SRS engine throws on a missing card) and a captured Personal sense has no
    // ingest step to create it, so the first read of an un-practiced card
    // materializes the seat — a deliberate, idempotent atomic insert-if-absent, not
    // an incidental write. Bulk to avoid an N+1 over a deck. (Materializing instead
    // at confirm would pull the SRS engine + persistence into vocabulary-editor's
    // domain module, a worse boundary than this localized seam.)
    private suspend fun materializeAll(cards: List<Card>): List<Card> {
        if (cards.isEmpty()) return cards
        val stored = srsStorage.getCards(cards.map { SrsCardId(it.id.value) })
        return cards.map { card ->
            card.copy(srs = stored[SrsCardId(card.id.value)] ?: srsStorage.saveCardIfAbsent(card.srs))
        }
    }
}

private fun List<StoredSense>.personalConfirmed(): List<StoredSense> =
    filter { it.status == SenseStatus.Confirmed && it.origin == SenseOrigin.Personal }

// Insert-if-absent then refresh metadata, so a Service re-sync never clears a
// deck's `subscribed` flag (see CatalogEntity.sq).
private suspend fun CatalogEntityQueries.upsertDeckMeta(
    id: String,
    title: String,
    description: String,
    cardCount: Long,
) {
    insertDeckIfAbsent(id = id, title = title, description = description, card_count = cardCount)
    updateDeckMeta(title = title, description = description, card_count = cardCount, id = id)
}

// Subscribed material is the user's own (Personal) to practice; an unsubscribed
// row is a passive Service-catalog suggestion.
private fun deck(
    id: String,
    title: String,
    description: String,
    subscribed: Long,
    cardCount: Long,
): Deck =
    Deck(
        id = DeckId(id),
        title = title,
        description = description,
        cardCount = cardCount.toInt(),
        origin = if (subscribed != 0L) CatalogOrigin.Personal else CatalogOrigin.Service,
    )
