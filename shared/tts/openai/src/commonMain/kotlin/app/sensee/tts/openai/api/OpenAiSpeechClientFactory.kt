package app.sensee.tts.openai.api

import app.sensee.core.network.NetworkConfig
import app.sensee.core.network.NetworkHttpClientFactory
import app.sensee.core.network.NetworkLogger
import app.sensee.core.network.NoOpNetworkLogger
import app.sensee.tts.openai.config.OpenAiSpeechConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json

/**
 * A dedicated [HttpClient] for OpenAI speech (Bearer auth, distinct base URL);
 * not the shared client. Auth is applied per-request inside [OpenAiSpeechApi].
 */
public object OpenAiSpeechClientFactory {
    public fun create(
        engine: HttpClientEngine,
        config: OpenAiSpeechConfig,
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

    private const val REQUEST_TIMEOUT_MILLIS: Long = 60_000L
}
