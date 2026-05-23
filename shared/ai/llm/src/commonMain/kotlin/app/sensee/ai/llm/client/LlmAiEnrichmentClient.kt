package app.sensee.ai.llm.client

import app.sensee.ai.core.AiEnrichmentClient
import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.ai.core.EnrichmentResponseMapper
import app.sensee.ai.core.EnrichmentResponseV1
import app.sensee.ai.core.EnrichmentResult
import app.sensee.ai.core.EnrichmentSchema
import app.sensee.ai.core.SenseCoverage
import app.sensee.ai.llm.api.LlmEnrichmentApi
import app.sensee.ai.llm.config.AiCredentialsProvider
import app.sensee.ai.llm.config.LlmConfig
import app.sensee.ai.llm.config.LlmConfigProvider
import app.sensee.ai.llm.dto.ChatMessageDto
import app.sensee.ai.llm.dto.JsonObjectResponseFormat
import app.sensee.ai.llm.dto.jsonSchemaResponseFormat
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Real LLM-backed enrichment over any OpenAI-compatible chat endpoint. Honors
 * the seam contract (ADR-005): no key, an unreachable provider, or an
 * unparseable answer all degrade into a first-class result — this never throws
 * across the seam, so the wizard always has a manual fallback.
 *
 * The raw model answer is parsed through the versioned wire DTO and the
 * anti-corruption mapper; it never reaches feature domain directly.
 */
public class LlmAiEnrichmentClient internal constructor(
    private val api: LlmEnrichmentApi,
    private val credentials: AiCredentialsProvider,
    private val configProvider: LlmConfigProvider,
    private val json: Json,
) : AiEnrichmentClient {
    override suspend fun enrich(request: EnrichmentRequest): EnrichmentResult {
        val apiKey =
            credentials.apiKey()?.takeIf { it.isNotBlank() }
                ?: return EnrichmentResult.unavailable("AI provider is not configured")

        return try {
            val config = configProvider.config()
            val responseFormat =
                if (config.structuredOutput) jsonSchemaResponseFormat(json) else JsonObjectResponseFormat
            val basePrompt = promptFor(request)
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
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            EnrichmentResult.unavailable(
                "AI provider unreachable: ${throwable.message ?: throwable::class.simpleName}",
            )
        }
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
        val dto =
            try {
                json.decodeFromString<EnrichmentResponseV1>(content)
            } catch (_: SerializationException) {
                return EnrichmentResult(
                    EnrichmentAvailability.Degraded("provider response was not in the expected format"),
                )
            }
        return EnrichmentResponseMapper.map(dto)
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
            KNOWN_POLYSEMY_HINTS[request.term.trim().lowercase()]?.let {
                append(' ')
                append(it)
            }
        }

    private fun promptFor(request: EnrichmentRequest): List<ChatMessageDto> {
        val fieldGuide =
            EnrichmentSchema.fields.joinToString("\n") { "- ${it.serialName}: ${it.guidance}" }
        return listOf(
            ChatMessageDto(role = "system", content = systemPromptFor(request, fieldGuide)),
            ChatMessageDto(role = "user", content = userPromptFor(request)),
        )
    }

    // Per-field semantics live in EnrichmentSchema (single source of truth);
    // cross-cutting rules (sense splitting, request-driven language
    // autodetection) are assembled here.
    private fun systemPromptFor(
        request: EnrichmentRequest,
        fieldGuide: String,
    ): String =
        "You are a lexicographer. Return ONLY JSON matching this schema: " +
            "${EnrichmentSchema.jsonSkeleton}.\n" +
            senseSplittingPrompt() +
            languagePrompt(request) + "\n" +
            coverageRule(request.senseCoverage) + "\n" +
            topicPreferencePrompt(request) +
            "For example, \"come across\" should normally include separate " +
            "senses for: find or meet by chance; seem or give a particular " +
            "impression, often \"come across as <adjective/noun>\"; be " +
            "communicated or understood clearly, often " +
            "\"<message/meaning/idea> comes across\".\n" +
            "Fields:\n$fieldGuide\n" +
            "Omit unknown fields."

    private fun senseSplittingPrompt(): String =
        "Each item is exactly one distinct sense — never merge senses into " +
            "one blob. Before producing the final JSON, identify the common " +
            "learner-relevant sense inventory of the input and return ALL common " +
            "distinct senses, not only the most frequent one — do not stop after " +
            "the first valid sense. For phrasal/prepositional verbs, idioms, " +
            "phrases and fixed expressions check whether the unit has multiple " +
            "common meanings, constructions or argument patterns. Return one item " +
            "only when there is genuinely only one common learner-relevant sense; " +
            "do not invent rare, obsolete or artificial senses to inflate the " +
            "count. A meaning-changing nuance is a separate sense, not a label. " +
            "YOU detect the lexical unit type (word, inflected form, phrasal / " +
            "prepositional / phrasal-prepositional verb, phrase, idiom, " +
            "collocation, fixed expression) — the user never declares it. " +
            "A particle or preposition that changes the meaning makes a SEPARATE " +
            "item (look at / look after / look for are different senses), never " +
            "an alternative; a preposition that keeps the meaning is " +
            "preposition_government; a fixed part of the unit (look down on) " +
            "belongs in surface_form. Conversely, do NOT over-split: one sense " +
            "whose complement may be a person or a thing stays ONE item (come " +
            "across an old friend / some letters is one sense) — write the slot " +
            "generically as <someone/something>, never split by object type. " +
            "Every item has at least one example, one per significant construction. " +
            "For an irregular verb include irregular_forms. "

    private fun languagePrompt(request: EnrichmentRequest): String =
        "Auto-detect the input language: it may be ${request.studyLanguageTag} " +
            "or ${request.nativeLanguageTag}. Always return ${request.studyLanguageTag} " +
            "senses; for ${request.nativeLanguageTag} input find the matching " +
            "${request.studyLanguageTag} variants. 'translation', 'explanation' " +
            "and 'usage_note' are in ${request.nativeLanguageTag} (the learner " +
            "picks senses by them); 'examples' are in ${request.studyLanguageTag} " +
            "with the studied unit in [[ ]]."

    private fun userPromptFor(request: EnrichmentRequest): String =
        buildString {
            append("Term: ${request.term}")
            request.userNote?.takeIf { it.isNotBlank() }?.let { append("\nUser note: $it") }
        }

    // Soft steering of 'examples' only. Topic keywords come from the learner's
    // settings (see RoutingAiEnrichmentClient); an empty list leaves the prompt
    // unchanged so unset preferences cost nothing.
    private fun topicPreferencePrompt(request: EnrichmentRequest): String {
        val topics = request.topicPreferences.filter { it.isNotBlank() }
        if (topics.isEmpty()) {
            return ""
        }
        return "The learner is interested in these topics: ${topics.joinToString(", ")}. " +
            "When a sense naturally allows it, prefer 'examples' set in those topics; " +
            "never force an unnatural or misleading context, and never let topic " +
            "steering distort the sense, translation, grammar or any non-example field.\n"
    }

    private fun coverageRule(coverage: SenseCoverage): String =
        when (coverage) {
            SenseCoverage.Minimal ->
                "Coverage: return only the most important sense(s) for a quick add."
            SenseCoverage.Common ->
                "Coverage: usually return 2-5 items for polysemous words, phrasal " +
                    "verbs, idioms, phrases and fixed expressions; return all common " +
                    "learner-relevant senses, avoiding rare or obsolete ones unless " +
                    "important for learners."
            SenseCoverage.Comprehensive ->
                "Coverage: return as many useful distinct dictionary senses as " +
                    "practical, still avoiding rare or obsolete senses unless " +
                    "important for learners."
        }
}

// Targeted retry hints for units that are reliably polysemous but that models
// often collapse to one sense. Kept tiny and explicit on purpose — not a
// dictionary, just a nudge for the known-bad cases (reasonable minimum).
private val KNOWN_POLYSEMY_HINTS: Map<String, String> =
    mapOf(
        "come across" to
            "The input \"come across\" is polysemous. Return separate items for: " +
            "find/meet by chance; seem/give an impression, often " +
            "\"come across as <adjective/noun>\"; be communicated/understood, often " +
            "\"<message/meaning/idea> comes across\".",
    )
