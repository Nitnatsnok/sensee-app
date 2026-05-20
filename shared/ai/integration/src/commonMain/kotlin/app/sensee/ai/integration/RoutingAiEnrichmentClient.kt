package app.sensee.ai.integration

import app.sensee.ai.core.AiEnrichmentClient
import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.fixture.FixtureAiEnrichmentClient
import app.sensee.ai.llm.client.LlmAiEnrichmentClient
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.settings.domain.UserSettingsRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

/**
 * The single bound [AiEnrichmentClient]. Selection is by configuration, not by
 * fallback-on-failure: a configured key routes to the live LLM (over the real
 * network engine, not the app's mock), an absent key routes to the offline
 * fixture. When the LLM is reachable but unhappy it returns a first-class
 * `Unavailable`/`Degraded` (ADR-005) and the wizard degrades to manual — the
 * router does not silently mask that with fixture output.
 *
 * Both delegates are app singletons; the LLM client holds one long-lived
 * HttpClient (Ktor best practice) and resolves endpoint/key per request.
 */
@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<AiEnrichmentClient>(),
)
@Inject
public class RoutingAiEnrichmentClient(
    private val fixture: FixtureAiEnrichmentClient,
    private val llm: LlmAiEnrichmentClient,
    private val settings: UserSettingsRepository,
    appDiagnostics: AppDiagnostics,
) : AiEnrichmentClient {
    private val logger = appDiagnostics.logger.tag("AiEnrichment")

    override suspend fun enrich(request: EnrichmentRequest): EnrichmentResult {
        val hasKey =
            !settings
                .readSettings()
                .ai.aiApiKey
                .isNullOrBlank()
        logger.debug { "Routing enrichment via ${if (hasKey) "LLM provider" else "offline fixture"}" }

        val result = if (hasKey) llm.enrich(request) else fixture.enrich(request)

        when (val availability = result.availability) {
            is EnrichmentAvailability.Unavailable ->
                logger.warn { "Enrichment unavailable, wizard degrades to manual: ${availability.reason}" }
            is EnrichmentAvailability.Degraded ->
                logger.warn { "Enrichment degraded (${result.suggestions.size} usable): ${availability.reason}" }
            EnrichmentAvailability.Available ->
                logger.debug { "Enrichment available with ${result.suggestions.size} suggestion(s)" }
        }
        return result
    }
}
