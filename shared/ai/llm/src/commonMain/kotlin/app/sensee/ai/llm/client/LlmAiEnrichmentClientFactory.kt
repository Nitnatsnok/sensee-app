package app.sensee.ai.llm.client

import app.sensee.ai.core.AiEnrichmentExtension
import app.sensee.ai.core.EnrichmentRequestModifier
import app.sensee.ai.core.UserEnrichmentPreferencesProvider
import app.sensee.ai.llm.api.LlmEnrichmentApi
import app.sensee.ai.llm.config.AiCredentialsProvider
import app.sensee.ai.llm.config.LlmConfig
import app.sensee.ai.llm.config.LlmConfigProvider
import app.sensee.core.network.NetworkConfig
import app.sensee.core.network.NetworkHttpClientFactory
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json

/**
 * Builds a single, long-lived [LlmAiEnrichmentClient]. The HttpClient is created
 * once and reused for the app lifetime (Ktor best practice) — base URL and model
 * are resolved per request, so a single instance serves runtime config changes
 * without being recreated or closed mid-flight.
 *
 * Its own client (not the shared one) is required: the LLM endpoint uses bearer
 * auth and must hit the real network, not the app's mock engine.
 */
public object LlmAiEnrichmentClientFactory {
    @Suppress("ProfiledLongParameterList")
    public fun create(
        engine: HttpClientEngine,
        credentials: AiCredentialsProvider,
        configProvider: LlmConfigProvider,
        taxonomyInvariantsProvider: TaxonomyInvariantsProvider,
        modifiers: Set<EnrichmentRequestModifier>,
        preferencesProvider: UserEnrichmentPreferencesProvider,
        json: Json,
        extensions: Set<AiEnrichmentExtension> = emptySet(),
    ): LlmAiEnrichmentClient =
        LlmAiEnrichmentClient(
            api =
                LlmEnrichmentApi(
                    httpClient =
                        NetworkHttpClientFactory.create(
                            engine = engine,
                            // Per-request absolute URLs override this; kept only for timeouts.
                            config =
                                NetworkConfig(
                                    baseUrl = LlmConfig.DEFAULT_BASE_URL,
                                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS,
                                ),
                            json = json,
                        ),
                ),
            credentials = credentials,
            configProvider = configProvider,
            taxonomyInvariantsProvider = taxonomyInvariantsProvider,
            modifiers = modifiers,
            extensions = extensions,
            preferencesProvider = preferencesProvider,
            json = json,
        )

    private const val REQUEST_TIMEOUT_MILLIS: Long = 60_000L
}
