package app.sensee.feature.library.data.remote

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

@Serializable
public data class CardDto(
    val id: String,
    @SerialName("lemma_id") val lemmaId: String,
    val headword: String,
    val translation: String,
    @SerialName("context_sentence") val contextSentence: String,
    @SerialName("unit_type") val unitType: String,
    @SerialName("grammar_tags") val grammarTags: List<GrammarTagDto> = emptyList(),
    @SerialName("sense_summary") val senseSummary: String,
    val explanation: String = "",
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
