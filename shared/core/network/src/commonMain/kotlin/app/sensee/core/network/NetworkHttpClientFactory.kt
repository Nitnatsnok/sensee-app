package app.sensee.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

public object NetworkHttpClientFactory {
    public fun create(
        config: NetworkConfig,
        json: Json,
        headersProvider: NetworkHeadersProvider = EmptyNetworkHeadersProvider,
        logger: NetworkLogger = NoOpNetworkLogger,
        logBodies: Boolean = false,
    ): HttpClient =
        HttpClient {
            applyNetworkDefaults(
                config = config,
                json = json,
                headersProvider = headersProvider,
                logger = logger,
                logBodies = logBodies,
            )
        }

    public fun <T : HttpClientEngineConfig> create(
        engineFactory: HttpClientEngineFactory<T>,
        config: NetworkConfig,
        json: Json,
        headersProvider: NetworkHeadersProvider = EmptyNetworkHeadersProvider,
        logger: NetworkLogger = NoOpNetworkLogger,
        logBodies: Boolean = false,
    ): HttpClient =
        HttpClient(engineFactory) {
            applyNetworkDefaults(
                config = config,
                json = json,
                headersProvider = headersProvider,
                logger = logger,
                logBodies = logBodies,
            )
        }

    public fun create(
        engine: HttpClientEngine,
        config: NetworkConfig,
        json: Json,
        headersProvider: NetworkHeadersProvider = EmptyNetworkHeadersProvider,
        logger: NetworkLogger = NoOpNetworkLogger,
        logBodies: Boolean = false,
    ): HttpClient =
        HttpClient(engine) {
            applyNetworkDefaults(
                config = config,
                json = json,
                headersProvider = headersProvider,
                logger = logger,
                logBodies = logBodies,
            )
        }
}

private fun <T : HttpClientEngineConfig> HttpClientConfig<T>.applyNetworkDefaults(
    config: NetworkConfig,
    json: Json,
    headersProvider: NetworkHeadersProvider,
    logger: NetworkLogger,
    logBodies: Boolean,
) {
    expectSuccess = true

    defaultRequest {
        url(config.baseUrl)
        headersProvider
            .headers()
            .forEach { (name, value) ->
                header(name, value)
            }
    }

    install(ContentNegotiation) {
        json(json)
    }

    install(HttpTimeout) {
        requestTimeoutMillis = config.requestTimeoutMillis
        connectTimeoutMillis = config.connectTimeoutMillis
        socketTimeoutMillis = config.socketTimeoutMillis
    }

    install(Logging) {
        this.logger = KtorNetworkLogger(logger)
        // Default INFO logs the request line and status only; [logBodies] opts into
        // full request/response bodies (e.g. LLM prompt debugging).
        level = if (logBodies) LogLevel.BODY else LogLevel.INFO
        // Never let a bearer token reach the logs, even at BODY level.
        sanitizeHeader { header -> header.equals(HttpHeaders.Authorization, ignoreCase = true) }
    }
}
