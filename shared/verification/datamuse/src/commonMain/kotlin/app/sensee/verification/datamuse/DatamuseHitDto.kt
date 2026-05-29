package app.sensee.verification.datamuse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One hit from `/words?sp=...`. [word] is the suggestion, [score] is the
 * Datamuse-internal rank — bigger is better. The exact upstream shape is
 * stable but our parsing tolerates extra fields silently.
 */
@Serializable
internal data class DatamuseHitDto(
    @SerialName("word") val word: String,
    @SerialName("score") val score: Int = 0,
)
