package app.sensee.feature.library.data.remote

import app.sensee.ai.core.EnrichmentItemV1
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

// The card IS a projection of its Sense: the enrichment fields sit flat on the
// card (no wrapper) with only the catalog identity (id, lemma_id) added, and the
// lean catalog columns (headword, translation, context, unit type, grammar tags,
// summary) are derived from it on sync (CatalogLocalDataSource.upsertCardInternal).
// The flat wire shape is handled by [CardDtoSerializer]; [enrichment] stays a
// typed EnrichmentItemV1 in code, so there is no field duplication.
@Serializable(with = CardDtoSerializer::class)
public data class CardDto(
    val id: String,
    val lemmaId: String,
    val enrichment: EnrichmentItemV1,
)

@Serializable
public data class LemmaDto(
    val id: String,
    val text: String,
    @SerialName("related_cards") val relatedCards: List<CardSummaryDto>,
)

@Serializable
public data class CardSummaryDto(
    val id: String,
    val headword: String,
    @SerialName("unit_type") val unitType: String,
    val translation: String,
    @SerialName("sense_summary") val senseSummary: String,
)

@Serializable
public data class GrammarTagDto(
    val category: String,
    val form: String,
)
