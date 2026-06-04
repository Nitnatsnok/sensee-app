package app.sensee.ai.llm.client

import app.sensee.ai.core.contract.AiEnrichmentExtension
import app.sensee.ai.core.request.EnrichmentRequestModifier
import app.sensee.ai.core.request.UserEnrichmentPreferencesProvider
import app.sensee.ai.llm.api.LlmEnrichmentApi
import app.sensee.ai.llm.config.AiCredentialsProvider
import app.sensee.ai.llm.config.LlmConfigProvider
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

/**
 * Builds a single, long-lived [LlmAiEnrichmentClient] over the shared LLM
 * [httpClient] (see [app.sensee.ai.llm.api.LlmHttpClientFactory]). Base URL and
 * model are resolved per request, so one instance serves runtime config changes
 * without being recreated mid-flight.
 */
public object LlmAiEnrichmentClientFactory {
    @Suppress("ProfiledLongParameterList")
    public fun create(
        httpClient: HttpClient,
        credentials: AiCredentialsProvider,
        configProvider: LlmConfigProvider,
        taxonomyInvariantsProvider: TaxonomyInvariantsProvider,
        modifiers: Set<EnrichmentRequestModifier>,
        preferencesProvider: UserEnrichmentPreferencesProvider,
        json: Json,
        extensions: Set<AiEnrichmentExtension> = emptySet(),
    ): LlmAiEnrichmentClient =
        LlmAiEnrichmentClient(
            api = LlmEnrichmentApi(httpClient = httpClient),
            credentials = credentials,
            configProvider = configProvider,
            taxonomyInvariantsProvider = taxonomyInvariantsProvider,
            modifiers = modifiers,
            extensions = extensions,
            preferencesProvider = preferencesProvider,
            json = json,
        )
}
