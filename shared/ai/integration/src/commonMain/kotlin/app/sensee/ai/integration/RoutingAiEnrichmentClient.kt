package app.sensee.ai.integration

import app.sensee.ai.core.AiEnrichmentClient
import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.core.SenseCoverage
import app.sensee.ai.curatedEnrichment.CuratedAiEnrichmentClient
import app.sensee.ai.llm.client.LlmAiEnrichmentClient
import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.settings.domain.TopicCatalogRepository
import app.sensee.settings.domain.UserSettingsRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

/**
 * The single bound [AiEnrichmentClient]: curated first, LLM second. See
 * `shared/ai/AGENTS.md` (Routing) for the layer contract.
 */
@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<AiEnrichmentClient>(),
)
@Inject
public class RoutingAiEnrichmentClient(
    private val curated: CuratedAiEnrichmentClient,
    private val llm: LlmAiEnrichmentClient,
    private val settings: UserSettingsRepository,
    private val topicCatalog: TopicCatalogRepository,
    appDiagnostics: AppDiagnostics,
) : AiEnrichmentClient {
    private val logger = appDiagnostics.logger.tag("AiEnrichment")

    override suspend fun enrich(request: EnrichmentRequest): EnrichmentResult {
        val route = request.prepareRoute()
        val curatedResult = enrichWithCuratedIfEligible(route)
        if (curatedResult.isCompleteCuratedHit()) {
            logger.debug {
                "Curated layer covered '${route.request.term}' with ${curatedResult.suggestions.size} suggestion(s)"
            }
            return curatedResult
        }

        if (!route.hasKey) {
            return unavailableWithoutKey(route.request, curatedResult)
        }

        val llmResult = enrichWithLlm(route.request)
        if (llmResult.suggestions.isEmpty() && curatedResult.suggestions.isNotEmpty()) {
            logger.warn { "LLM enrichment returned no suggestions; falling back to degraded curated suggestions" }
            return curatedResult
        }
        return llmResult
    }

    private suspend fun EnrichmentRequest.prepareRoute(): EnrichmentRoute {
        val snapshot =
            runCatchingCancellable {
                settings.readSettings()
            }.getOrElse { throwable ->
                logger.warn {
                    "AI settings unavailable (${throwable.message ?: throwable::class.simpleName}); " +
                        "enrichment proceeds as no-key"
                }
                return EnrichmentRoute(request = this, hasKey = false)
            }
        val hasKey = !snapshot.ai.aiApiKey.isNullOrBlank()
        val hasSavedTopicPreferences = snapshot.learning.preferredTopicIds.isNotEmpty()
        val request =
            if (hasKey) {
                withTopicPreferences(snapshot.learning.preferredTopicIds)
            } else {
                this
            }
        return EnrichmentRoute(
            request = request,
            hasKey = hasKey,
            skipCurated = hasKey && hasSavedTopicPreferences,
        )
    }

    private suspend fun enrichWithCuratedIfEligible(route: EnrichmentRoute): EnrichmentResult =
        if (route.skipCurated) {
            logger.debug { "Skipping curated enrichment because saved topic preferences require LLM steering" }
            EnrichmentResult(EnrichmentAvailability.Available, emptyList())
        } else if (route.request.canUseCuratedLayer()) {
            curated.enrich(route.request)
        } else {
            logger.debug { "Skipping curated enrichment because request needs LLM-specific handling" }
            EnrichmentResult(EnrichmentAvailability.Available, emptyList())
        }

    private fun unavailableWithoutKey(
        request: EnrichmentRequest,
        curatedResult: EnrichmentResult,
    ): EnrichmentResult {
        if (curatedResult.availability is EnrichmentAvailability.Degraded) {
            logger.warn { "Curated enrichment degraded and no AI key is configured" }
            return curatedResult
        }
        logger.debug {
            "No AI configured and curated layer did not cover '${request.term}'; reporting Unavailable"
        }
        return EnrichmentResult(
            availability =
                EnrichmentAvailability.Unavailable(
                    "no AI configured and no curated coverage for '${request.term}'",
                ),
            suggestions = emptyList(),
        )
    }

    private suspend fun enrichWithLlm(request: EnrichmentRequest): EnrichmentResult {
        logger.debug { "Routing enrichment via LLM provider" }
        val result = llm.enrich(request)
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

    private data class EnrichmentRoute(
        val request: EnrichmentRequest,
        val hasKey: Boolean,
        val skipCurated: Boolean = false,
    )

    private fun EnrichmentResult.isCompleteCuratedHit(): Boolean =
        availability is EnrichmentAvailability.Available && suggestions.isNotEmpty()

    private fun EnrichmentRequest.canUseCuratedLayer(): Boolean =
        studyLanguageTag.lowercase() in CURATED_STUDY_LANGUAGES &&
            nativeLanguageTag.lowercase() in CURATED_NATIVE_LANGUAGES &&
            userNote.isNullOrBlank() &&
            senseCoverage == SenseCoverage.Common &&
            topicPreferences.isEmpty()

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
        runCatchingCancellable {
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
        }.getOrElse { throwable ->
            logger.warn {
                "Topic catalog unavailable (${throwable.message ?: throwable::class.simpleName}); " +
                    "enrichment proceeds without topic steering"
            }
            emptyList()
        }

    private companion object {
        val CURATED_STUDY_LANGUAGES: Set<String> = setOf("en", "en-us", "en-gb")
        val CURATED_NATIVE_LANGUAGES: Set<String> = setOf("ru", "ru-ru")
    }
}
