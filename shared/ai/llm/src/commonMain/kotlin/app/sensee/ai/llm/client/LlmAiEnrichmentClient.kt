package app.sensee.ai.llm.client

import app.sensee.ai.core.contract.AiEnrichmentClient
import app.sensee.ai.core.contract.AiEnrichmentExtension
import app.sensee.ai.core.contract.EnrichmentAvailability
import app.sensee.ai.core.contract.EnrichmentResult
import app.sensee.ai.core.contract.extractItemExtensions
import app.sensee.ai.core.request.EnrichmentPromptAssembler
import app.sensee.ai.core.request.EnrichmentRequest
import app.sensee.ai.core.request.EnrichmentRequestContext
import app.sensee.ai.core.request.EnrichmentRequestModifier
import app.sensee.ai.core.request.EnrichmentSchema
import app.sensee.ai.core.request.EnrichmentTaxonomy
import app.sensee.ai.core.request.PolysemyHintsModifier
import app.sensee.ai.core.request.SenseCoverage
import app.sensee.ai.core.request.UserEnrichmentPreferencesProvider
import app.sensee.ai.core.wire.EnrichmentResponseMapper
import app.sensee.ai.core.wire.EnrichmentResponseV1
import app.sensee.ai.llm.api.LlmEnrichmentApi
import app.sensee.ai.llm.config.AiCredentialsProvider
import app.sensee.ai.llm.config.LlmConfig
import app.sensee.ai.llm.config.LlmConfigProvider
import app.sensee.ai.llm.dto.ChatMessageDto
import app.sensee.ai.llm.dto.JsonObjectResponseFormat
import app.sensee.ai.llm.dto.jsonSchemaResponseFormat
import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.grammar.domain.TaxonomyInvariants
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * LLM-backed enrichment over any OpenAI-compatible chat endpoint. Honors the
 * seam contract (ADR-005): no key, an unreachable provider, or an unparseable
 * answer all degrade into a first-class result — never throws across the seam.
 *
 * Prompt content comes from [EnrichmentRequestModifier]s, assembled per call;
 * schema constraints from [EnrichmentSchema.buildJsonSchema] over the loaded
 * taxonomy; the answer passes through [EnrichmentResponseMapper] before
 * reaching the seam.
 */
@Suppress("ProfiledLongParameterList")
public class LlmAiEnrichmentClient internal constructor(
    private val api: LlmEnrichmentApi,
    private val credentials: AiCredentialsProvider,
    private val configProvider: LlmConfigProvider,
    private val taxonomyInvariantsProvider: TaxonomyInvariantsProvider,
    private val modifiers: Set<EnrichmentRequestModifier>,
    private val extensions: Set<AiEnrichmentExtension>,
    private val preferencesProvider: UserEnrichmentPreferencesProvider,
    private val json: Json,
) : AiEnrichmentClient {
    override suspend fun enrich(request: EnrichmentRequest): EnrichmentResult {
        return runCatchingCancellable {
            val apiKey =
                credentials.apiKey()?.takeIf { it.isNotBlank() }
                    ?: return EnrichmentResult.unavailable("AI provider is not configured")
            val config = configProvider.config()
            val invariants = taxonomyInvariantsProvider.invariants() ?: TaxonomyInvariants.EMPTY
            val responseFormat = responseFormatFor(config, invariants)
            val context =
                EnrichmentRequestContext(
                    request = request,
                    preferences = preferencesProvider.preferences(),
                )
            val basePrompt = promptFor(context)
            val first = callAndMap(apiKey, config, responseFormat, basePrompt)
            if (!shouldRetryForMoreSenses(request, first)) {
                return first
            }
            // A single item under non-minimal coverage is suspicious for a
            // polysemous unit. One corrective retry only — keep the richer
            // answer; never punish a genuinely monosemous unit (no minItems).
            val retried =
                callAndMap(
                    apiKey,
                    config,
                    responseFormat,
                    basePrompt + ChatMessageDto(role = "user", content = retryAppendix(request)),
                )
            if (retried.suggestions.size > first.suggestions.size) retried else first
        }.getOrElse { throwable ->
            EnrichmentResult.unavailable(
                "AI provider unreachable: ${throwable.message ?: throwable::class.simpleName}",
            )
        }
    }

    private fun responseFormatFor(
        config: LlmConfig,
        invariants: TaxonomyInvariants,
    ): JsonObject =
        if (config.structuredOutput) {
            jsonSchemaResponseFormat(
                EnrichmentSchema.buildJsonSchema(
                    taxonomy =
                        EnrichmentTaxonomy(
                            unitTypeIds = invariants.knownUnitTypeIds,
                            complementIds = invariants.knownComplementIds,
                            usageAxesAndValues = invariants.allowedValuesByAxis,
                            grammarCategoriesAndForms = invariants.allowedFormsByCategory,
                        ),
                    extensions = extensions,
                ),
            )
        } else {
            JsonObjectResponseFormat
        }

    private suspend fun callAndMap(
        apiKey: String,
        config: LlmConfig,
        responseFormat: JsonObject,
        messages: List<ChatMessageDto>,
    ): EnrichmentResult {
        val response = api.complete(apiKey, config.baseUrl, config.model, messages, responseFormat)
        val content =
            response.choices
                .firstOrNull()
                ?.message
                ?.content
                ?.takeIf { it.isNotBlank() }
                ?: return EnrichmentResult(
                    EnrichmentAvailability.Degraded("provider returned an empty response"),
                )
        return try {
            val element = json.parseToJsonElement(content)
            val dto = json.decodeFromJsonElement(EnrichmentResponseV1.serializer(), element)
            EnrichmentResponseMapper.map(dto, extensionValuesFrom(element))
        } catch (_: SerializationException) {
            EnrichmentResult(
                EnrichmentAvailability.Degraded("provider response was not in the expected format"),
            )
        }
    }

    private fun extensionValuesFrom(element: JsonElement): List<Map<String, JsonElement>> {
        if (extensions.isEmpty()) return emptyList()
        val items = (element as? JsonObject)?.get("items") as? JsonArray ?: return emptyList()
        return items.map { item ->
            (item as? JsonObject)?.let { extensions.extractItemExtensions(it) }.orEmpty()
        }
    }

    private fun shouldRetryForMoreSenses(
        request: EnrichmentRequest,
        result: EnrichmentResult,
    ): Boolean =
        request.senseCoverage != SenseCoverage.Minimal &&
            result.availability !is EnrichmentAvailability.Unavailable &&
            result.suggestions.size == 1

    private fun retryAppendix(request: EnrichmentRequest): String =
        buildString {
            append(
                "Your previous response returned only one sense. Re-check whether \"",
            )
            append(request.term)
            append(
                "\" has additional common learner-relevant senses. If it has " +
                    "multiple common senses, return them ALL as separate items, not " +
                    "only the new one. For phrasal verbs, idioms, phrases and fixed " +
                    "expressions pay special attention to figurative meanings, " +
                    "different constructions and different argument patterns. Do not " +
                    "duplicate the existing sense; add only genuinely distinct common " +
                    "senses. Return one item only if there is genuinely only one " +
                    "common learner-relevant sense.",
            )
            PolysemyHintsModifier.polysemyHintFor(request.term)?.let {
                append(' ')
                append(it)
            }
        }

    private fun promptFor(context: EnrichmentRequestContext): List<ChatMessageDto> {
        val request = context.request
        val assembled = EnrichmentPromptAssembler.assemble(modifiers, context)
        val systemPrompt = assembled.systemFragments.joinToString(separator = "\n")
        val userPrompt =
            buildString {
                append("Term: ${request.term}")
                request.userNote?.takeIf { it.isNotBlank() }?.let { append("\nUser note: $it") }
                assembled.userFragments.forEach { fragment ->
                    append('\n')
                    append(fragment)
                }
            }
        return listOf(
            ChatMessageDto(role = "system", content = systemPrompt),
            ChatMessageDto(role = "user", content = userPrompt),
        )
    }
}
