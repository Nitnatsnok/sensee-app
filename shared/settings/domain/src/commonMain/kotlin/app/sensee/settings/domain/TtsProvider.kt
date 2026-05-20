package app.sensee.settings.domain

/**
 * A compatible text-to-speech provider chosen from a fixed list. Unlike the
 * AI-chat seam, TTS providers are NOT API-compatible (ElevenLabs and OpenAI
 * have different endpoints/auth), so each has its own adapter behind the
 * `Speaker` contract; this enum only carries the user-facing choice and its
 * predefined endpoint + known models/voices (offline fallback for the picker).
 */
public enum class TtsProvider(
    public val id: String,
    public val displayName: String,
    public val baseUrl: String,
    public val knownModels: List<String>,
    public val knownVoices: List<String>,
) {
    ElevenLabs(
        id = "elevenlabs",
        displayName = "ElevenLabs",
        baseUrl = "https://api.elevenlabs.io/",
        knownModels = listOf("eleven_multilingual_v2", "eleven_turbo_v2_5"),
        knownVoices = emptyList(),
    ),
    OpenAi(
        id = "openai",
        displayName = "OpenAI",
        baseUrl = "https://api.openai.com/",
        knownModels = listOf("gpt-4o-mini-tts", "tts-1", "tts-1-hd"),
        knownVoices = listOf("alloy", "echo", "fable", "onyx", "nova", "shimmer"),
    ),
    ;

    public companion object {
        public val Default: TtsProvider = ElevenLabs

        public fun fromId(id: String?): TtsProvider = entries.firstOrNull { it.id == id } ?: Default
    }
}
