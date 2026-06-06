package app.sensee.ai.llm.client

import app.sensee.ai.llm.api.LlmEmbeddingApi
import app.sensee.ai.llm.config.AiCredentialsProvider
import app.sensee.ai.llm.config.LlmConfigProvider
import io.ktor.client.HttpClient

/**
 * Builds a single, long-lived [LlmAiEmbeddingClient] over the shared LLM
 * [httpClient] (see [app.sensee.ai.llm.api.LlmHttpClientFactory]). The embeddings
 * endpoint uses bearer auth and resolves the base URL per request.
 */
public object LlmAiEmbeddingClientFactory {
    public fun create(
        httpClient: HttpClient,
        credentials: AiCredentialsProvider,
        configProvider: LlmConfigProvider,
    ): LlmAiEmbeddingClient =
        LlmAiEmbeddingClient(
            api = LlmEmbeddingApi(httpClient),
            credentials = credentials,
            configProvider = configProvider,
        )
}
