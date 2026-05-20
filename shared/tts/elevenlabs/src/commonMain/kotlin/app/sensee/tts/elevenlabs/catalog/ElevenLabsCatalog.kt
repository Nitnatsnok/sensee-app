package app.sensee.tts.elevenlabs.catalog

import app.sensee.tts.elevenlabs.api.ElevenLabsApi
import app.sensee.tts.elevenlabs.config.ElevenLabsConfig
import app.sensee.tts.elevenlabs.config.ElevenLabsCredentialsProvider
import io.ktor.client.HttpClient

/**
 * Live model/voice listing for ElevenLabs (`/v1/models`, `/v1/voices`). Returns
 * provider ids; callers degrade to an offline set if these throw (the seam
 * boundary owns the never-throw guarantee, like the AI model catalog).
 */
public class ElevenLabsCatalog internal constructor(
    private val api: ElevenLabsApi,
) {
    public suspend fun modelIds(): List<String> = api.models().map { it.modelId }

    public suspend fun voiceIds(): List<String> = api.voices().voices.map { it.voiceId }
}

public object ElevenLabsCatalogFactory {
    public fun create(
        httpClient: HttpClient,
        config: ElevenLabsConfig,
        credentials: ElevenLabsCredentialsProvider,
    ): ElevenLabsCatalog = ElevenLabsCatalog(ElevenLabsApi(httpClient, config, credentials))
}
