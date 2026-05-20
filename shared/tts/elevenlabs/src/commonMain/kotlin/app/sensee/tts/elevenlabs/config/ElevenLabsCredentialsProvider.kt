package app.sensee.tts.elevenlabs.config

/**
 * Provides the API key for ElevenLabs authentication.
 *
 * The provider is suspend because credentials may come from asynchronous
 * storage or token exchange.
 */
public fun interface ElevenLabsCredentialsProvider {
    public suspend fun apiKey(): String
}

public class StaticElevenLabsCredentialsProvider(
    private val apiKey: String,
) : ElevenLabsCredentialsProvider {
    override suspend fun apiKey(): String = apiKey
}
