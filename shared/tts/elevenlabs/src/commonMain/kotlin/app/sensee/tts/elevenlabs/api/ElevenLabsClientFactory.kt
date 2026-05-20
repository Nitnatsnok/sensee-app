package app.sensee.tts.elevenlabs.api

import app.sensee.core.network.NetworkConfig
import app.sensee.core.network.NetworkHttpClientFactory
import app.sensee.core.network.NetworkLogger
import app.sensee.core.network.NoOpNetworkLogger
import app.sensee.tts.elevenlabs.config.ElevenLabsConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json

/**
 * Builds a [HttpClient] dedicated to ElevenLabs. We don't reuse the shared
 * client because the base URL and auth header (`xi-api-key`) are distinct.
 *
 * Authentication is applied per-request inside [ElevenLabsApi] rather than via
 * a default header because credential lookup is suspendable.
 */
public object ElevenLabsClientFactory {
    public fun create(
        engine: HttpClientEngine,
        config: ElevenLabsConfig,
        json: Json,
        logger: NetworkLogger = NoOpNetworkLogger,
    ): HttpClient =
        NetworkHttpClientFactory.create(
            engine = engine,
            config =
                NetworkConfig(
                    baseUrl = config.baseUrl,
                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS,
                ),
            json = json,
            logger = logger,
        )

    /** Audio synthesis can take seconds; allow a longer ceiling than the default. */
    private const val REQUEST_TIMEOUT_MILLIS: Long = 60_000L
}
