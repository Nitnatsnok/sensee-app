package app.sensee.feature.library.data.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.sensee.ai.core.EnrichmentItemV1
import app.sensee.ai.core.EnrichmentResponseMapper
import app.sensee.ai.core.EnrichmentResponseV1
import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.database.CatalogEntityQueries
import app.sensee.core.database.Practice_card
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.logging.AppLogger
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.library.data.remote.CardSummaryDto
import app.sensee.feature.library.data.remote.DeckDto
import app.sensee.feature.library.data.remote.DeckSummaryDto
import app.sensee.feature.library.data.remote.LemmaDto
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
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.enrichment.toSense
import app.sensee.lexicon.serialization.SenseDto
import app.sensee.lexicon.serialization.toDomain
import app.sensee.lexicon.serialization.toDto
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.model.SrsCardSnapshot
import app.sensee.srs.engine.factory.SrsCardFactory
import app.sensee.srs.engine.storage.SrsStorage
import app.sensee.srs.fsrs.FsrsParameters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Catalog-side local store. Reads card content from the SQLDelight catalog tables; the per-card
 * SRS snapshot is fetched through the [SrsStorage] interface (owned by Practice) so the catalog
 * layer never reaches into the SRS table directly — keeping the dependency on the SRS contract,
 * not the practice data implementation.
 */
@SingleIn(AppScope::class)
@Inject
public class CatalogLocalDataSource(
    private val databaseProvider: SenseeDatabaseProvider,
    private val srsStorage: SrsStorage<FsrsParameters>,
    private val dispatchers: AppDispatchers,
    private val json: Json,
    private val clock: Clock,
    appDiagnostics: AppDiagnostics,
) {
    private val logger: AppLogger = appDiagnostics.logger.tag("CatalogLocalDataSource")

    public suspend fun upsertDeckSummaries(decks: List<DeckSummaryDto>) {
        val database = databaseProvider.database()
        decks.forEach { deck ->
            database.catalogEntityQueries.upsertDeckMeta(
                deck.id,
                deck.title,
                deck.description,
                deck.cardCount.toLong(),
            )
        }
    }

    public suspend fun upsertDeckWithCards(deck: DeckDto) {
        val database = databaseProvider.database()
        val now = clock.now().toEpochMilliseconds()
        // clearDeckCards + re-insert must be atomic: a partial failure leaves the
        // deck row surviving but empty, and loadDeck returns that empty state.
        database.transaction {
            database.catalogEntityQueries.upsertDeckMeta(
                deck.id,
                deck.title,
                deck.description,
                deck.cards.size.toLong(),
            )
            deck.cards.forEach { card ->
                database.upsertCardInternal(card, now, json, srsStorage)
            }
            database.catalogEntityQueries.clearDeckCards(deck.id)
            deck.cards.forEachIndexed { index, card ->
                database.catalogEntityQueries.insertDeckCard(
                    deck_id = deck.id,
                    card_id = card.id,
                    position = index.toLong(),
                )
            }
        }
    }

    public suspend fun upsertLemma(lemma: LemmaDto) {
        databaseProvider.database().catalogEntityQueries.upsertLemma(
            id = lemma.id,
            text = lemma.text,
        )
    }

    public suspend fun selectDecks(): List<Deck> =
        databaseProvider
            .database()
            .catalogEntityQueries
            .selectAllDecks()
            .awaitAsList()
            .map { row ->
                deck(row.id, row.title, row.description, row.adopted_at_epoch_ms, row.card_count)
            }

    public suspend fun selectOwnedDecks(): List<Deck> =
        databaseProvider
            .database()
            .catalogEntityQueries
            .selectOwnedDecks()
            .awaitAsList()
            .map { row ->
                deck(row.id, row.title, row.description, row.adopted_at_epoch_ms, row.card_count)
            }

    public fun observeAllDecks(): Flow<List<Deck>> =
        flow {
            val database = databaseProvider.database()
            emitAll(
                database.catalogEntityQueries
                    .selectAllDecks()
                    .asFlow()
                    .mapToList(dispatchers.io)
                    .map { rows ->
                        rows.map { row ->
                            deck(row.id, row.title, row.description, row.adopted_at_epoch_ms, row.card_count)
                        }
                    },
            )
        }

    public fun observeOwnedDecks(): Flow<List<Deck>> =
        flow {
            val database = databaseProvider.database()
            emitAll(
                database.catalogEntityQueries
                    .selectOwnedDecks()
                    .asFlow()
                    .mapToList(dispatchers.io)
                    .map { rows ->
                        rows.map { row ->
                            deck(row.id, row.title, row.description, row.adopted_at_epoch_ms, row.card_count)
                        }
                    },
            )
        }

    public suspend fun markDeckAdopted(deckId: String) {
        databaseProvider.database().catalogEntityQueries.markDeckAdopted(
            adopted_at_epoch_ms = clock.now().toEpochMilliseconds(),
            id = deckId,
        )
    }

    public suspend fun clearDeckAdopted(deckId: String) {
        databaseProvider.database().catalogEntityQueries.clearDeckAdopted(id = deckId)
    }

    public suspend fun selectDeckWithCards(deckId: String): DeckWithCards? {
        val database = databaseProvider.database()
        val deckRow =
            database.catalogEntityQueries
                .selectDeckById(deckId)
                .awaitAsOneOrNull()
                ?: return null
        val entities =
            database.catalogEntityQueries
                .selectCardsByDeck(deckId)
                .awaitAsList()
        val snapshots = srsStorage.getCards(entities.map { SrsCardId(it.id) })
        val cards =
            entities.map { entity ->
                val cardId = SrsCardId(entity.id)
                entity.toCard(json, snapshots[cardId] ?: SrsCardFactory.newCard(cardId), logger)
            }
        return DeckWithCards(
            deck =
                deck(
                    deckRow.id,
                    deckRow.title,
                    deckRow.description,
                    deckRow.adopted_at_epoch_ms,
                    deckRow.card_count,
                ),
            cards = cards,
        )
    }

    public suspend fun selectCard(cardId: String): Card? {
        val database = databaseProvider.database()
        val entity =
            database.catalogEntityQueries
                .selectCardById(cardId)
                .awaitAsOneOrNull()
                ?: return null
        return entity.toCard(json, srsSnapshot(srsStorage, cardId), logger)
    }

    public suspend fun selectLemma(lemmaId: String): Lemma? {
        val database = databaseProvider.database()
        val row =
            database.catalogEntityQueries
                .selectLemmaById(lemmaId)
                .awaitAsOneOrNull()
                ?: return null
        val cardRows =
            database.catalogEntityQueries
                .selectCardsByLemma(lemmaId)
                .awaitAsList()
        return Lemma(
            id = LemmaId(row.id),
            text = row.text,
            relatedCards = cardRows.map { it.toCardSummary() },
            derivatives = derivativesOf(cardRows, json, logger),
        )
    }
}

private suspend fun srsSnapshot(
    srsStorage: SrsStorage<FsrsParameters>,
    cardId: String,
): SrsCardSnapshot = srsStorage.getCard(SrsCardId(cardId)) ?: SrsCardFactory.newCard(SrsCardId(cardId))

// Insert-if-absent then refresh metadata, so a Service re-sync never clears
// a deck's adopted_at_epoch_ms (see CatalogEntity.sq).
private suspend fun CatalogEntityQueries.upsertDeckMeta(
    id: String,
    title: String,
    description: String,
    cardCount: Long,
) {
    insertDeckIfAbsent(id = id, title = title, description = description, card_count = cardCount)
    updateDeckMeta(title = title, description = description, card_count = cardCount, id = id)
}

private suspend fun SenseeDatabase.upsertCardInternal(
    card: app.sensee.feature.library.data.remote.CardDto,
    nowEpochMs: Long,
    json: Json,
    srsStorage: SrsStorage<FsrsParameters>,
) {
    // Card has an FK to practice_lemma(id); make sure the row exists. The real
    // text comes through the separate loadLemma path; here we only seed an
    // id-derived placeholder, and IF-ABSENT so we never overwrite a real one.
    catalogEntityQueries.insertLemmaIfAbsent(
        id = card.lemmaId,
        text = card.lemmaId.removePrefix("lemma-"),
    )
    // The lean columns are a projection of the card's Sense, not a second
    // source of truth: map the ideal enrichment into a Sense once here (the same
    // boundary mappers the read path and capture use), persist the Sense as
    // sense_json, and derive the flashcard essentials from it (mirrors
    // CapturedCatalogDerivation). sense_json is the stored source of truth.
    val fallbackTerm =
        card.enrichment.surfaceForm?.takeIf { it.isNotBlank() }
            ?: card.lemmaId.removePrefix("lemma-")
    val sense = card.enrichment.toConfirmedSenseOrNull(fallbackTerm)
    val unitType = sense?.unitType ?: GrammarUnitType.Phrase
    val contextSentence =
        sense
            ?.contextualApplications
            ?.firstOrNull()
            ?.sentence
            ?.marked()
            .orEmpty()
    catalogEntityQueries.upsertCard(
        id = card.id,
        lemma_id = card.lemmaId,
        headword = sense?.surfaceForm?.display() ?: fallbackTerm,
        translation = sense?.translation ?: card.enrichment.translation.orEmpty(),
        context_sentence = contextSentence,
        unit_type = unitType.id,
        grammar_tags_json = encodeGrammarTags(json, sense?.grammarTags?.map { it.toDto() }.orEmpty()),
        sense_summary = sense?.explanation ?: sense?.translation ?: card.enrichment.translation.orEmpty(),
        explanation = sense?.explanation.orEmpty(),
        sense_json = encodeSense(json, sense),
        created_at_epoch_ms = nowEpochMs,
        updated_at_epoch_ms = nowEpochMs,
    )
    srsStorage.saveCardIfAbsent(SrsCardFactory.newCard(SrsCardId(card.id)))
}

private fun encodeSense(
    json: Json,
    sense: Sense?,
): String = sense?.let { json.encodeToString(SenseDto.serializer(), it.toDto()) } ?: "{}"

// Maps an ideal-enrichment item into a confirmed Sense through the same boundary
// mappers capture uses: enrichment wire -> neutral suggestion -> Sense. Done
// ONCE at sync; the result is persisted as a SenseDto, so
// reads never re-map. Returns null only when the item has no usable translation
// (the mapper drops it), so the card degrades to its lean columns.
private fun EnrichmentItemV1.toConfirmedSenseOrNull(fallbackTerm: String): Sense? =
    EnrichmentResponseMapper
        .map(EnrichmentResponseV1(items = listOf(this)))
        .suggestions
        .firstOrNull()
        ?.toSense(fallbackTerm = fallbackTerm)

// Reads the persisted Sense — the same SenseDto shape the capture path stores.
// A blank/'{}' payload (a legacy lean row) or a malformed one yields null, so
// the lean columns still work.
private fun Practice_card.senseOrNull(
    json: Json,
    logger: AppLogger,
): Sense? {
    if (sense_json.isBlank() || sense_json == "{}") return null
    return try {
        json.decodeFromString(SenseDto.serializer(), sense_json).toDomain()
    } catch (failure: SerializationException) {
        logger.warn(failure) { "Malformed sense_json for card $id; no rich sense" }
        null
    }
}

// The lemma page's word family: derivatives every related card's rich sense
// declared, deduped by lemma (mirrors CapturedCatalogDerivation).
private fun derivativesOf(
    cardRows: List<Practice_card>,
    json: Json,
    logger: AppLogger,
): List<LemmaDerivative> {
    val seen = mutableSetOf<String>()
    return cardRows
        .flatMap { it.senseOrNull(json, logger)?.wordFamily.orEmpty() }
        .mapNotNull { member ->
            val text = member.lemma.trim()
            if (text.isEmpty() || !seen.add(text.lowercase())) null else LemmaDerivative(text, member.unitType)
        }
}

private fun Practice_card.toCard(
    json: Json,
    srs: SrsCardSnapshot,
    logger: AppLogger,
): Card =
    Card(
        id = CardId(id),
        lemmaId = LemmaId(lemma_id),
        headword = headword,
        translation = translation,
        contextSentence = context_sentence,
        unitType = parseUnitType(unit_type),
        grammarTags = parseGrammarTags(json, grammar_tags_json, id, logger),
        senseSummary = sense_summary,
        explanation = explanation,
        sense = senseOrNull(json, logger),
        srs = srs,
    )

// Adoption is provenance, not a copy: an adopted deck is the user's own
// (Personal) material; an un-adopted row is a passive Service-catalog cache.
private fun deck(
    id: String,
    title: String,
    description: String,
    adoptedAt: Long?,
    cardCount: Long,
): Deck =
    Deck(
        id = DeckId(id),
        title = title,
        description = description,
        cardCount = cardCount.toInt(),
        origin = if (adoptedAt != null) CatalogOrigin.Personal else CatalogOrigin.Service,
    )

private fun Practice_card.toCardSummary(): CardSummary =
    CardSummary(
        id = CardId(id),
        headword = headword,
        unitType = parseUnitType(unit_type),
        translation = translation,
        senseSummary = sense_summary,
    )

private fun CardSummaryDto.toCardSummary(): CardSummary =
    CardSummary(
        id = CardId(id),
        headword = headword,
        unitType = parseUnitType(unitType),
        translation = translation,
        senseSummary = senseSummary,
    )

internal fun LemmaDto.toLemma(): Lemma =
    Lemma(
        id = LemmaId(id),
        text = text,
        relatedCards = relatedCards.map(CardSummaryDto::toCardSummary),
    )

private fun parseUnitType(raw: String): GrammarUnitType = GrammarUnitType.fromId(raw)
