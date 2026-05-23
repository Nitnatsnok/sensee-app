package app.sensee.ai.integration

import app.sensee.ai.core.AiEnrichmentClient
import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.fixture.FixtureAiEnrichmentClient
import app.sensee.ai.llm.client.LlmAiEnrichmentClient
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.settings.domain.TopicCatalogRepository
import app.sensee.settings.domain.UserSettingsRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CancellationException

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
    private val topicCatalog: TopicCatalogRepository,
    appDiagnostics: AppDiagnostics,
) : AiEnrichmentClient {
    private val logger = appDiagnostics.logger.tag("AiEnrichment")

    override suspend fun enrich(request: EnrichmentRequest): EnrichmentResult {
        val snapshot = settings.readSettings()
        val hasKey = !snapshot.ai.aiApiKey.isNullOrBlank()
        logger.debug { "Routing enrichment via ${if (hasKey) "LLM provider" else "offline fixture"}" }

        val result =
            if (hasKey) {
                llm.enrich(request.withTopicPreferences(snapshot.learning.preferredTopicIds))
            } else {
                fixture.enrich(request)
            }

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

    // Topic preferences are a global user setting, not a per-call input, so the
    // router threads them in — the capture caller stays unaware of them. A
    // catalog hiccup degrades to no steering; it never blocks enrichment.
    private suspend fun EnrichmentRequest.withTopicPreferences(preferredTopicIds: Set<String>): EnrichmentRequest {
        if (preferredTopicIds.isEmpty()) {
            return this
        }
        val keywords = resolveTopicKeywords(preferredTopicIds)
        if (keywords.isEmpty()) {
            return this
        }
        val mergedKeywords = (topicPreferences + keywords).filter { it.isNotBlank() }.distinct()
        logger.debug { "Applied ${mergedKeywords.size} topic keyword(s) to enrichment request" }
        return copy(topicPreferences = mergedKeywords)
    }

    private suspend fun resolveTopicKeywords(preferredTopicIds: Set<String>): List<String> =
        try {
            val resolved =
                topicCatalog
                    .topics()
                    .filter { it.id in preferredTopicIds }
                    .map { it.promptKeyword }
            if (resolved.isEmpty()) {
                logger.debug {
                    "Preferred topic ids (${preferredTopicIds.size}) did not match any catalog entry; " +
                        "enrichment proceeds without topic steering"
                }
            }
            resolved
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logger.warn {
                "Topic catalog unavailable (${throwable.message ?: throwable::class.simpleName}); " +
                    "enrichment proceeds without topic steering"
            }
            emptyList()
        }
}
