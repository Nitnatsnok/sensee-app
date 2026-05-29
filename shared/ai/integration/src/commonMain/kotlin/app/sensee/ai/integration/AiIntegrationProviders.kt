package app.sensee.ai.integration

import app.sensee.ai.core.AiEnrichmentExtension
import app.sensee.ai.core.AiModelCatalog
import app.sensee.ai.core.DefaultAiEnrichmentExtensions
import app.sensee.ai.core.DefaultEnrichmentRequestModifiers
import app.sensee.ai.core.EnrichmentRequestModifier
import app.sensee.ai.core.UserEnrichmentPreferences
import app.sensee.ai.core.UserEnrichmentPreferencesProvider
import app.sensee.ai.llm.api.LlmHttpClientFactory
import app.sensee.ai.llm.catalog.LlmModelCatalogFactory
import app.sensee.ai.llm.client.LlmAiEnrichmentClient
import app.sensee.ai.llm.client.LlmAiEnrichmentClientFactory
import app.sensee.core.network.createRealHttpClientEngine
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

@ContributesTo(AppScope::class)
public interface AiIntegrationProviders {
    /**
     * One real-network [HttpClient] for all LLM calls (enrichment + model
     * catalog), created once for the app lifetime so both share one client/pool.
     */
    @LlmHttpClient
    @SingleIn(AppScope::class)
    @Provides
    public fun provideLlmHttpClient(json: Json): HttpClient =
        LlmHttpClientFactory.create(engine = createRealHttpClientEngine(), json = json)

    /**
     * One real-network LLM client for the app lifetime (Ktor best practice):
     * the HttpClient is created once and never recycled mid-flight; endpoint and
     * key are resolved per request from settings.
     */
    @Suppress("ProfiledLongParameterList")
    @SingleIn(AppScope::class)
    @Provides
    public fun provideLlmAiEnrichmentClient(
        @LlmHttpClient httpClient: HttpClient,
        credentials: SettingsBackedAiCredentialsProvider,
        configProvider: SettingsLlmConfigProvider,
        taxonomyInvariantsProvider: TaxonomyInvariantsProvider,
        modifiers: Set<EnrichmentRequestModifier>,
        extensions: Set<AiEnrichmentExtension>,
        preferencesProvider: UserEnrichmentPreferencesProvider,
        json: Json,
    ): LlmAiEnrichmentClient =
        LlmAiEnrichmentClientFactory.create(
            httpClient = httpClient,
            credentials = credentials,
            configProvider = configProvider,
            taxonomyInvariantsProvider = taxonomyInvariantsProvider,
            modifiers = modifiers,
            preferencesProvider = preferencesProvider,
            json = json,
            extensions = extensions,
        )

    /** One real-network model catalog for the app lifetime; the typed key is passed per call. */
    @SingleIn(AppScope::class)
    @Provides
    public fun provideAiModelCatalog(
        @LlmHttpClient httpClient: HttpClient,
    ): AiModelCatalog = LlmModelCatalogFactory.create(httpClient = httpClient)

    @SingleIn(AppScope::class)
    @Provides
    public fun provideEnrichmentRequestModifiers(): Set<EnrichmentRequestModifier> = DefaultEnrichmentRequestModifiers

    @SingleIn(AppScope::class)
    @Provides
    public fun provideAiEnrichmentExtensions(): Set<AiEnrichmentExtension> = DefaultAiEnrichmentExtensions

    @SingleIn(AppScope::class)
    @Provides
    public fun provideUserEnrichmentPreferencesProvider(): UserEnrichmentPreferencesProvider =
        UserEnrichmentPreferencesProvider { UserEnrichmentPreferences.EMPTY }
}

/**
 * Qualifies the real-network [HttpClient] for LLM calls (enrichment + model
 * catalog), distinct from the mock-backend [HttpClient] bound as the default.
 */
@Qualifier
public annotation class LlmHttpClient
