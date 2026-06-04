package app.sensee.ai.llm.catalog

import app.sensee.ai.core.contract.AiKeyCheck
import app.sensee.ai.core.contract.AiModelCatalog
import app.sensee.core.coroutines.runCatchingCancellable
import io.ktor.client.plugins.ResponseException

/**
 * Verifies a key over any OpenAI-compatible `/v1/models` endpoint and, on
 * success, returns the chat-suitable models. Honors the seam contract
 * (ADR-005): a blank key, a rejected key, or an unreachable provider all
 * become a first-class [AiKeyCheck.Invalid] — this never throws.
 *
 * The raw `/v1/models` listing mixes in embeddings, TTS, transcription,
 * image, moderation and base models that cannot do the lexicographer
 * chat-completion task; on top of that it carries dated snapshots
 * (`gpt-4o-2024-05-13`, `-1106-preview`, …) and superseded families
 * (`gpt-3.5`, `-instruct`, `vision-preview`). Both are filtered out so the
 * picker stays short and offers only canonical, current chat aliases. This is
 * a heuristic, not a hardcoded allowlist — it stays provider-agnostic
 * (OpenRouter `vendor/model` ids survive) and needs no upkeep when new models
 * ship. An empty result is still [AiKeyCheck.Valid] (the key works, the
 * provider just exposes no usable chat model — the UI then offers manual entry).
 */
public class LlmModelCatalog internal constructor(
    private val api: LlmModelsApi,
) : AiModelCatalog {
    override suspend fun verifyKey(
        baseUrl: String,
        apiKey: String,
    ): AiKeyCheck {
        if (apiKey.isBlank()) {
            return AiKeyCheck.Invalid("API key is empty")
        }
        return runCatchingCancellable {
            when (val fetch = api.fetchModelIds(apiKey, baseUrl)) {
                is ModelsFetch.Ok -> AiKeyCheck.Valid(fetch.ids.filter(::isChatSuitable))
                is ModelsFetch.Rejected ->
                    AiKeyCheck.Invalid(reasonFor(fetch.statusCode))
            }
        }.getOrElse { throwable ->
            when (throwable) {
                // expectSuccess=true surfaces non-2xx as an exception; classify it here too.
                is ResponseException -> AiKeyCheck.Invalid(reasonFor(throwable.response.status.value))
                else ->
                    AiKeyCheck.Invalid(
                        "Provider unreachable: ${throwable.message ?: throwable::class.simpleName}",
                    )
            }
        }
    }

    private fun reasonFor(statusCode: Int): String =
        when (statusCode) {
            401, 403 -> "API key was rejected by the provider"
            429 -> "Provider rate limit reached — try again shortly"
            else -> "Provider returned HTTP $statusCode"
        }

    private fun isChatSuitable(id: String): Boolean {
        val normalized = id.lowercase()
        if (NON_CHAT_MARKERS.any { it in normalized }) {
            return false
        }
        if (LEGACY_MARKERS.any { it in normalized } || DATED_SNAPSHOT.containsMatchIn(normalized)) {
            return false
        }
        return CHAT_PREFIXES.any { normalized.startsWith(it) } || normalized.contains('/')
    }

    private companion object {
        // Non-chat OpenAI-compatible families: embeddings, audio, image,
        // moderation, legacy base/completion models, and codex (a separate
        // /v1/responses code-only line, not chat completions).
        private val NON_CHAT_MARKERS =
            listOf(
                "embedding",
                "whisper",
                "tts",
                "audio",
                "transcribe",
                "realtime",
                "dall-e",
                "image",
                "moderation",
                "search",
                "babbage",
                "ada",
                "davinci",
                "curie",
                "codex",
            )

        // Superseded chat families that are still chat-capable but no longer
        // worth offering: pre-4 GPT, instruct/base completion variants, the
        // old vision preview (folded into 4o), gpt-4-turbo (superseded by
        // gpt-4.1/4o), gpt-4.5 preview (deprecated), and the o1 preview/mini
        // line (superseded by o3-mini and o3).
        private val LEGACY_MARKERS =
            listOf(
                "gpt-3.5",
                "gpt-3-",
                "-instruct",
                "-base",
                "vision-preview",
                "gpt-4-turbo",
                "gpt-4.5",
                "o1-preview",
                "o1-mini",
            )

        // Dated snapshots: `-0613`, `-1106-preview`, `-2024-05-13`, claude
        // `-20241022`, … Canonical aliases (gpt-4o, gpt-4.1, o3, o4-mini,
        // vendor/model) carry no `-NNNN` run, so this only drops pinned dates.
        private val DATED_SNAPSHOT = Regex("-\\d{4}")

        // Chat-completion families (OpenRouter ids are `vendor/model`, kept via the '/' check).
        private val CHAT_PREFIXES = listOf("gpt-", "chatgpt", "o1", "o3", "o4")
    }
}
