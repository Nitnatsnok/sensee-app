package app.sensee.ai.llm.api

import app.sensee.ai.llm.config.LlmConfig
import app.sensee.core.network.NetworkConfig
import app.sensee.core.network.NetworkHttpClientFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json

/**
 * Builds the single real-network [HttpClient] shared by the LLM enrichment
 * client and the model catalog. Both hit the same provider with bearer auth and
 * resolve the endpoint per request, so one app-lifetime instance serves both —
 * the base URL here is a placeholder overridden per request. The timeout covers
 * slow generations; the catalog's quicker key check rides the same ceiling.
 */
public object LlmHttpClientFactory {
    public fun create(
        engine: HttpClientEngine,
        json: Json,
    ): HttpClient =
        NetworkHttpClientFactory.create(
            engine = engine,
            config =
                NetworkConfig(
                    baseUrl = LlmConfig.DEFAULT_BASE_URL,
                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS,
                ),
            json = json,
        )

    private const val REQUEST_TIMEOUT_MILLIS: Long = 60_000L
}
