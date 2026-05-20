package app.sensee.ai.llm.catalog

import app.sensee.ai.llm.config.LlmConfig
import app.sensee.core.network.NetworkConfig
import app.sensee.core.network.NetworkHttpClientFactory
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json

/**
 * Builds a single, long-lived [LlmModelCatalog] over a real-network engine
 * (the models endpoint uses bearer auth and must not hit the app's mock).
 */
public object LlmModelCatalogFactory {
    public fun create(
        engine: HttpClientEngine,
        json: Json,
    ): LlmModelCatalog =
        LlmModelCatalog(
            api =
                LlmModelsApi(
                    httpClient =
                        NetworkHttpClientFactory.create(
                            engine = engine,
                            config =
                                NetworkConfig(
                                    baseUrl = LlmConfig.DEFAULT_BASE_URL,
                                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS,
                                ),
                            json = json,
                        ),
                ),
        )

    private const val REQUEST_TIMEOUT_MILLIS: Long = 30_000L
}
