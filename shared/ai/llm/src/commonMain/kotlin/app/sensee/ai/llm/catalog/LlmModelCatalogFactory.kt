package app.sensee.ai.llm.catalog

import io.ktor.client.HttpClient

/**
 * Builds a single, long-lived [LlmModelCatalog] over the shared LLM
 * [httpClient] (see [app.sensee.ai.llm.api.LlmHttpClientFactory]). The models
 * endpoint uses bearer auth and resolves the base URL per request.
 */
public object LlmModelCatalogFactory {
    public fun create(httpClient: HttpClient): LlmModelCatalog =
        LlmModelCatalog(
            api = LlmModelsApi(httpClient = httpClient),
        )
}
