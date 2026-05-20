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
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

public object NetworkHttpClientFactory {
    public fun create(
        config: NetworkConfig,
        json: Json,
        headersProvider: NetworkHeadersProvider = EmptyNetworkHeadersProvider,
        logger: NetworkLogger = NoOpNetworkLogger,
    ): HttpClient =
        HttpClient {
            applyNetworkDefaults(
                config = config,
                json = json,
                headersProvider = headersProvider,
                logger = logger,
            )
        }

    public fun <T : HttpClientEngineConfig> create(
        engineFactory: HttpClientEngineFactory<T>,
        config: NetworkConfig,
        json: Json,
        headersProvider: NetworkHeadersProvider = EmptyNetworkHeadersProvider,
        logger: NetworkLogger = NoOpNetworkLogger,
    ): HttpClient =
        HttpClient(engineFactory) {
            applyNetworkDefaults(
                config = config,
                json = json,
                headersProvider = headersProvider,
                logger = logger,
            )
        }

    public fun create(
        engine: HttpClientEngine,
        config: NetworkConfig,
        json: Json,
        headersProvider: NetworkHeadersProvider = EmptyNetworkHeadersProvider,
        logger: NetworkLogger = NoOpNetworkLogger,
    ): HttpClient =
        HttpClient(engine) {
            applyNetworkDefaults(
                config = config,
                json = json,
                headersProvider = headersProvider,
                logger = logger,
            )
        }
}

private fun <T : HttpClientEngineConfig> HttpClientConfig<T>.applyNetworkDefaults(
    config: NetworkConfig,
    json: Json,
    headersProvider: NetworkHeadersProvider,
    logger: NetworkLogger,
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
        level = LogLevel.INFO
    }
}
