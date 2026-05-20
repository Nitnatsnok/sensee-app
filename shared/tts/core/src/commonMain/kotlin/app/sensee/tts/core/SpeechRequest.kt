package app.sensee.tts.core

import kotlin.jvm.JvmInline

public data class SpeechRequest(
    val text: String,
    val locale: SpeechLocale,
    val voiceId: VoiceId? = null,
    val modelId: String? = null,
    val rate: SpeechRate = SpeechRate.Normal,
    val quality: SpeechQuality = SpeechQuality.Fast,
) {
    init {
        require(text.isNotBlank()) { "SpeechRequest text must not be blank" }
    }
}

public enum class SpeechQuality {
    Fast,

    High,
}

@JvmInline
public value class SpeechRate(
    public val multiplier: Float,
) {
    init {
        require(multiplier in MIN..MAX) {
            "SpeechRate must be in [$MIN, $MAX], got $multiplier"
        }
    }

    public companion object {
        public const val MIN: Float = 0.5f
        public const val MAX: Float = 2.0f

        public val Slow: SpeechRate = SpeechRate(0.75f)
        public val Normal: SpeechRate = SpeechRate(1.0f)
        public val Fast: SpeechRate = SpeechRate(1.25f)
    }
}
