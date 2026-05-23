package app.sensee.settings.data.topic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class TopicCatalogDto(
    val topics: List<TopicDto>,
)

@Serializable
public data class TopicDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("prompt_keyword") val promptKeyword: String,
)
