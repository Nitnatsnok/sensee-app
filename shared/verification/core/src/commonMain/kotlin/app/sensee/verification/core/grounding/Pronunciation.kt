package app.sensee.verification.core.grounding

import kotlinx.serialization.Serializable

@Serializable
public data class PronunciationInfo(
    val variants: List<PronunciationVariant>,
)

@Serializable
public data class PronunciationVariant(
    val accent: String? = null,
    val ipa: String? = null,
    val audioUrl: String? = null,
)
