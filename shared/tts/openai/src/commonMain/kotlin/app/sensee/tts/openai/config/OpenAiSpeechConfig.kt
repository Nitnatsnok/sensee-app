package app.sensee.tts.openai.config

/**
 * Endpoint config for OpenAI's `POST /v1/audio/speech`. Unlike ElevenLabs the
 * voice is a fixed known id (alloy/echo/...) and the model is a small fixed
 * set; both are resolved per request with a default here.
 */
public data class OpenAiSpeechConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val model: String = DEFAULT_MODEL,
    val defaultVoice: String = DEFAULT_VOICE,
    val responseFormat: String = DEFAULT_RESPONSE_FORMAT,
) {
    public companion object {
        public const val DEFAULT_BASE_URL: String = "https://api.openai.com/"
        public const val DEFAULT_MODEL: String = "gpt-4o-mini-tts"
        public const val DEFAULT_VOICE: String = "alloy"
        public const val DEFAULT_RESPONSE_FORMAT: String = "mp3"
    }
}
