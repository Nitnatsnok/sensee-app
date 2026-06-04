package app.sensee.verification.core.grounding

import kotlinx.serialization.Serializable

/**
 * Word-frequency evidence on a log scale. [zipf] is the canonical
 * Zipf-frequency value (6 ≈ every-utterance high-frequency, ≤2 ≈ rare); a
 * band derived from it rides alongside for a UI that does not want to plot
 * a number. Either field is optional: an adapter that knows only the band
 * leaves [zipf] null, and vice versa.
 */
@Serializable
public data class FrequencyScore(
    val zipf: Double? = null,
    val band: FrequencyBand? = null,
)

@Serializable
public enum class FrequencyBand {
    Top1k,
    Top3k,
    Top5k,
    Top10k,
    Top20k,
    Beyond20k,
    Unknown,
}
