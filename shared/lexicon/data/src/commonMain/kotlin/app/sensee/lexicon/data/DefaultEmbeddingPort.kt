package app.sensee.lexicon.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.sensee.ai.core.contract.AiEmbeddingClient
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.logging.AppLogger
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.lexicon.domain.EmbeddingPort
import app.sensee.lexicon.domain.EmbeddingVector
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.SenseId
import app.sensee.lexicon.domain.SenseReadRepository
import app.sensee.lexicon.domain.SenseStatus
import app.sensee.lexicon.domain.SimilarSense
import app.sensee.lexicon.domain.StoredSense
import app.sensee.lexicon.domain.cosineSimilarity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Embedding-backed similar-sense lookup over the canonical sense store (ADR-001,
 * a bolt-on): a separate class so it owns the `sense_embedding` table and the
 * cosine math without widening [DefaultSenseRepository]. The vector comes from
 * the [AiEmbeddingClient] seam; storage normalizes it to unit length and packs
 * it into a float32 BLOB.
 *
 * [embed] is best-effort and never throws for a provider miss (the client
 * returns `null` rather than throwing); the caller fires it after the sense row
 * has committed and within a bounded budget, so embedding never fails a save and
 * never blocks its round-trip. [findSimilar] gates
 * candidates to the query's `(modelRef, dim)` and resolves survivors through the
 * read repository, so an embedding whose sense row is gone is simply skipped.
 */
@SingleIn(AppScope::class)
@Inject
public class DefaultEmbeddingPort(
    private val databaseProvider: SenseeDatabaseProvider,
    private val senseReadRepository: SenseReadRepository,
    private val embeddingClient: AiEmbeddingClient,
    appDiagnostics: AppDiagnostics,
) : EmbeddingPort {
    private val logger: AppLogger = appDiagnostics.logger.tag("DefaultEmbeddingPort")

    override suspend fun embed(stored: StoredSense): EmbeddingVector? {
        // Embedding covers practiceable material; a Draft has no place in the
        // similar-sense index. Today's callers only pass Confirmed — this guards the
        // contract should a future one not.
        if (stored.status != SenseStatus.Confirmed) return null
        val text = stored.sense.embeddingText()
        if (text.isBlank()) return null
        val embedding = embeddingClient.embed(text) ?: return null
        val normalized = embedding.values.l2Normalized()
        if (normalized == null) {
            logger.warn { "Provider returned a zero vector for sense ${stored.id}; leaving it unembedded." }
            return null
        }
        databaseProvider.database().senseEmbeddingQueries.upsert(
            sense_id = stored.id.value,
            vector = normalized.toVectorBytes(),
            model_ref = embedding.model,
            dim = normalized.size.toLong(),
        )
        return EmbeddingVector(values = normalized, modelRef = embedding.model)
    }

    override suspend fun findSimilar(
        query: EmbeddingVector,
        excluding: SenseId,
        threshold: Float,
    ): List<SimilarSense> {
        val scored =
            databaseProvider
                .database()
                .senseEmbeddingQueries
                .selectByModel(model_ref = query.modelRef, dim = query.dim.toLong())
                .awaitAsList()
                .asSequence()
                .filter { it.sense_id != excluding.value }
                .mapNotNull { row ->
                    val candidate = row.vector.toFloatVector()
                    if (candidate.size != query.dim) return@mapNotNull null
                    val score = cosineSimilarity(query.values, candidate)
                    if (score >= threshold) SenseId(row.sense_id) to score else null
                }.sortedByDescending { it.second }
                .toList()
        return scored.mapNotNull { (id, score) ->
            senseReadRepository.getById(id)?.let { SimilarSense(it, score) }
        }
    }

    // The text handed to the embedding model: the L2 surface, its L1 translation,
    // and any gloss — the fields that carry a sense's meaning. Tuning detail of
    // this impl, not a contract.
    private fun Sense.embeddingText(): String =
        listOfNotNull(
            surfaceForm?.display(),
            translation.ifBlank { null },
            explanation?.ifBlank { null },
        ).joinToString(separator = " — ")
}
