package app.sensee.ai.llm.api

import app.sensee.ai.llm.config.LlmConfig
import app.sensee.core.network.NetworkConfig
import app.sensee.core.network.NetworkHttpClientFactory
import app.sensee.core.network.NetworkLogger
import app.sensee.core.network.NoOpNetworkLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json

/**
 * Builds the single real-network [HttpClient] shared by the LLM enrichment
 * client and the model catalog. Both hit the same provider with bearer auth and
 * resolve the endpoint per request, so one app-lifetime instance serves both —
 * the base URL here is a placeholder overridden per request. The timeouts cover a
 * full slow generation; the catalog's quicker key check rides the same ceiling.
 */
public object LlmHttpClientFactory {
    public fun create(
        engine: HttpClientEngine,
        json: Json,
        logger: NetworkLogger = NoOpNetworkLogger,
        // Full request/response body logging is a debug-build only tool — the prompt
        // sent and the raw answer received carry user input, so the caller passes the
        // build's debug flag here rather than logging bodies in release. The bearer
        // token is redacted by NetworkHttpClientFactory's sanitizeHeader regardless.
        logBodies: Boolean = false,
    ): HttpClient =
        NetworkHttpClientFactory.create(
            engine = engine,
            config =
                NetworkConfig(
                    baseUrl = LlmConfig.DEFAULT_BASE_URL,
                    requestTimeoutMillis = GENERATION_TIMEOUT_MILLIS,
                    // A non-streaming reasoning model (o-series, gpt-5) emits nothing
                    // while it thinks, so the socket sits idle for the whole generation.
                    // The idle-socket timeout must cover that, not the default 30s —
                    // otherwise it fires mid-think before the answer ever arrives.
                    socketTimeoutMillis = GENERATION_TIMEOUT_MILLIS,
                ),
            json = json,
            logger = logger,
            logBodies = logBodies,
        )

    // Reasoning models can think for a minute or more before the first byte; budget
    // for a full slow generation rather than a fast chat completion.
    private const val GENERATION_TIMEOUT_MILLIS: Long = 180_000L
}
