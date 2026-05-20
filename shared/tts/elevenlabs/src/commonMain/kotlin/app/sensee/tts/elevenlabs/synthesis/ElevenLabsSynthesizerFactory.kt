package app.sensee.tts.elevenlabs.synthesis

import app.sensee.tts.elevenlabs.api.ElevenLabsApi
import app.sensee.tts.elevenlabs.config.ElevenLabsConfig
import app.sensee.tts.elevenlabs.config.ElevenLabsCredentialsProvider
import io.ktor.client.HttpClient

public object ElevenLabsSynthesizerFactory {
    public fun create(
        httpClient: HttpClient,
        config: ElevenLabsConfig,
        credentials: ElevenLabsCredentialsProvider,
    ): ElevenLabsSynthesizer =
        ElevenLabsSynthesizer(
            api = ElevenLabsApi(httpClient, config, credentials),
            config = config,
        )
}
