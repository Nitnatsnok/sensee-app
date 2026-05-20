package app.sensee.tts.elevenlabs.config

import app.sensee.tts.core.SpeechLocale
import app.sensee.tts.core.SpeechQuality
import app.sensee.tts.core.VoiceId

public data class ElevenLabsConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val modelForFast: String = DEFAULT_FAST_MODEL,
    val modelForHigh: String = DEFAULT_HIGH_MODEL,
    val outputFormat: String = DEFAULT_OUTPUT_FORMAT,
    val defaultVoiceByLocale: Map<SpeechLocale, VoiceId> = emptyMap(),
    val stability: Double = DEFAULT_STABILITY,
    val similarityBoost: Double = DEFAULT_SIMILARITY_BOOST,
) {
    public fun modelFor(quality: SpeechQuality): String =
        when (quality) {
            SpeechQuality.Fast -> modelForFast
            SpeechQuality.High -> modelForHigh
        }

    public companion object {
        public const val DEFAULT_BASE_URL: String = "https://api.elevenlabs.io/"
        public const val DEFAULT_FAST_MODEL: String = "eleven_turbo_v2_5"
        public const val DEFAULT_HIGH_MODEL: String = "eleven_multilingual_v2"

        // mp3_44100_128 — broadly supported by platform players, no extra decoder needed.
        public const val DEFAULT_OUTPUT_FORMAT: String = "mp3_44100_128"

        public const val DEFAULT_STABILITY: Double = 0.5
        public const val DEFAULT_SIMILARITY_BOOST: Double = 0.7
    }
}
