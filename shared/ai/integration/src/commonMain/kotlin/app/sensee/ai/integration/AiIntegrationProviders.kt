package app.sensee.ai.integration

import app.sensee.ai.core.contract.AiEmbeddingClient
import app.sensee.ai.core.contract.AiEnrichmentExtension
import app.sensee.ai.core.contract.AiModelCatalog
import app.sensee.ai.core.model.DefaultAiEnrichmentExtensions
import app.sensee.ai.core.request.DefaultEnrichmentRequestModifiers
import app.sensee.ai.core.request.EnrichmentRequestModifier
import app.sensee.ai.core.request.UserEnrichmentPreferences
import app.sensee.ai.core.request.UserEnrichmentPreferencesProvider
import app.sensee.ai.llm.api.LlmHttpClientFactory
import app.sensee.ai.llm.catalog.LlmModelCatalogFactory
import app.sensee.ai.llm.client.LlmAiEmbeddingClientFactory
import app.sensee.ai.llm.client.LlmAiEnrichmentClient
import app.sensee.ai.llm.client.LlmAiEnrichmentClientFactory
import app.sensee.core.network.createRealHttpClientEngine
import app.sensee.core.observability.logging.AppLogger
import app.sensee.core.platform.PlatformEnvironment
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
    public fun provideLlmHttpClient(
        json: Json,
        logger: AppLogger,
        platformEnvironment: PlatformEnvironment,
    ): HttpClient {
        // Route Ktor's body logging into the app logger so the prompt sent and the
        // raw answer received (enrichment, embeddings, model catalog) surface in the
        // normal debug stream under one tag. Bodies carry user input, so they are
        // logged only on a debug build; the bearer token is redacted upstream anyway.
        val httpLogger = logger.tag("LlmHttp")
        return LlmHttpClientFactory.create(
            engine = createRealHttpClientEngine(),
            json = json,
            logger = { message -> httpLogger.debug { message } },
            logBodies = platformEnvironment.isDebug,
        )
    }

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

    /**
     * One real-network embedding client for the app lifetime: reuses the shared
     * LLM [HttpClient] (same provider, same pool) and resolves key and base URL
     * per request from settings. Embedding rides its own `/v1/embeddings` call,
     * not the enrichment wire (ADR-006).
     */
    @SingleIn(AppScope::class)
    @Provides
    public fun provideAiEmbeddingClient(
        @LlmHttpClient httpClient: HttpClient,
        credentials: SettingsBackedAiCredentialsProvider,
        configProvider: SettingsLlmConfigProvider,
    ): AiEmbeddingClient =
        LlmAiEmbeddingClientFactory.create(
            httpClient = httpClient,
            credentials = credentials,
            configProvider = configProvider,
        )

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
