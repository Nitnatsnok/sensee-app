package app.sensee.verification.datamuse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One hit from `/words?sp=...`. [word] is the suggestion, [score] is the
 * Datamuse-internal rank — bigger is better. Typed as `Long` because the
 * upstream score has no documented `Int` ceiling: large values surface
 * normally (see test fixtures using 120_000+), and a future score past
 * `Int.MAX_VALUE` would otherwise crash the whole spell-lookup with a
 * deserialization error and degrade the entire verifier leg. Our parsing
 * also tolerates extra fields silently.
 */
@Serializable
internal data class DatamuseHitDto(
    @SerialName("word") val word: String,
    @SerialName("score") val score: Long = 0L,
)
