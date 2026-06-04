package app.sensee.feature.library.data.remote

import app.sensee.lexicon.serialization.SenseDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class DeckListDto(
    val decks: List<DeckSummaryDto>,
)

@Serializable
public data class DeckSummaryDto(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("card_count") val cardCount: Int,
)

@Serializable
public data class DeckDto(
    val id: String,
    val title: String,
    val description: String,
    val cards: List<CardDto>,
)

// A service catalog card is its catalog identity plus the canonical [SenseDto].
// `id` is the sense's source_ref — the stable sense_id is derived from it, so a
// card shared across decks keeps a single identity. On ingest
// (CatalogLocalDataSource.ingestDeck) the sense maps straight through
// SenseDto.toDomain() into a Sense (origin = Service): the catalog stores no
// denormalized card columns and runs no enrichment chain.
@Serializable
public data class CardDto(
    val id: String,
    @SerialName("lemma_id") val lemmaId: String,
    val sense: SenseDto,
)
