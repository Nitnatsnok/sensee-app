package app.sensee.ai.llm.client

import app.sensee.ai.core.contract.AiEmbeddingClient
import app.sensee.ai.core.contract.Embedding
import app.sensee.ai.llm.api.LlmEmbeddingApi
import app.sensee.ai.llm.config.AiCredentialsProvider
import app.sensee.ai.llm.config.LlmConfigProvider
import app.sensee.core.coroutines.runCatchingCancellable

/**
 * Embeds text over an OpenAI-compatible `/v1/embeddings` endpoint — the same seam
 * tier as [LlmAiEnrichmentClient], but on its own call (ADR-006): the vector
 * never rides the enrichment wire. The key and base URL resolve per request from
 * settings (so a single long-lived client follows runtime changes); the model and
 * dimension are fixed here — tuning them mints new `model_ref`/`dim` rows with no
 * migration. The chat model from [LlmConfigProvider] is intentionally unused; only
 * the shared provider base URL is read.
 *
 * Honors the seam contract: a blank/absent key, an unreachable provider, a
 * rejected key, a provider whose base URL has no embeddings endpoint (a
 * non-OpenAI provider), or a malformed answer all degrade to `null` — never an
 * exception across the seam.
 */
public class LlmAiEmbeddingClient internal constructor(
    private val api: LlmEmbeddingApi,
    private val credentials: AiCredentialsProvider,
    private val configProvider: LlmConfigProvider,
) : AiEmbeddingClient {
    override suspend fun embed(text: String): Embedding? {
        if (text.isBlank()) return null
        return runCatchingCancellable {
            val key = credentials.apiKey()?.takeIf { it.isNotBlank() } ?: return@runCatchingCancellable null
            api
                .embed(
                    apiKey = key,
                    baseUrl = configProvider.config().baseUrl,
                    model = MODEL,
                    input = text,
                    dimensions = DIMENSIONS,
                ).data
                .firstOrNull()
                ?.embedding
                ?.takeIf { it.isNotEmpty() }
                ?.let { Embedding(values = it.toFloatArray(), model = MODEL_REF) }
        }.getOrNull()
    }

    public companion object {
        /** The OpenAI embedding model requested. */
        public const val MODEL: String = "text-embedding-3-small"

        /** Matryoshka dimension requested explicitly (~2 KB per sense at float32). */
        public const val DIMENSIONS: Int = 512

        /** The stored `model_ref` — namespaced so a later model swap gates old vectors out. */
        public const val MODEL_REF: String = "openai/text-embedding-3-small"
    }
}
