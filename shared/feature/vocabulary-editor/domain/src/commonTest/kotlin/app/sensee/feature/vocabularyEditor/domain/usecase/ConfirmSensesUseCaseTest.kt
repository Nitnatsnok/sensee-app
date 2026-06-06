package app.sensee.feature.vocabularyEditor.domain.usecase

import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.lexicon.domain.ContextualApplication
import app.sensee.lexicon.domain.EmbeddingPort
import app.sensee.lexicon.domain.EmbeddingVector
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.SenseId
import app.sensee.lexicon.domain.SenseOrigin
import app.sensee.lexicon.domain.SenseStatus
import app.sensee.lexicon.domain.SenseWriteRepository
import app.sensee.lexicon.domain.SimilarSense
import app.sensee.lexicon.domain.StoredSense
import app.sensee.lexicon.domain.WriteIntent
import app.sensee.lexicon.domain.deriveLemmaKey
import app.sensee.lexicon.domain.requireConfirmable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Confirm-then-embed orchestration: the batch is confirmed durably, each confirmed
 * sense is embedded, and an embedding failure is swallowed so it never fails the
 * confirm (the vector is a soft signal, persisted best-effort for later backfill).
 */
class ConfirmSensesUseCaseTest {
    private class FakeSenseWriteRepository(
        private val confirmFails: Boolean = false,
    ) : SenseWriteRepository {
        val confirmed: MutableList<Sense> = mutableListOf()

        override suspend fun upsert(
            sense: Sense,
            status: SenseStatus,
            origin: SenseOrigin,
            sourceRef: String?,
            intent: WriteIntent,
        ): StoredSense = error("ConfirmSensesUseCase confirms a batch; upsert is unused")

        override suspend fun confirmAll(senses: List<Sense>): List<StoredSense> {
            if (confirmFails) error("confirm rejected")
            senses.forEach { it.requireConfirmable() }
            confirmed += senses
            return senses.mapIndexed { index, sense -> stored("sense-$index", sense) }
        }
    }

    private class FakeEmbeddingPort(
        private val failing: Boolean = false,
        private val delayMs: Long = 0,
    ) : EmbeddingPort {
        val embedded: MutableList<SenseId> = mutableListOf()

        override suspend fun embed(stored: StoredSense): EmbeddingVector? {
            if (failing) error("embedding store offline")
            if (delayMs > 0) delay(delayMs)
            embedded += stored.id
            return EmbeddingVector(floatArrayOf(1f, 0f), "test-model")
        }

        override suspend fun findSimilar(
            query: EmbeddingVector,
            excluding: SenseId,
            threshold: Float,
        ): List<SimilarSense> = emptyList()
    }

    @Test
    fun `it confirms the batch and embeds each confirmed sense`() =
        runTest {
            val repo = FakeSenseWriteRepository()
            val embeddingPort = FakeEmbeddingPort()
            val useCase = ConfirmSensesUseCase(repo, embeddingPort, noOpAppDiagnostics())

            val confirmed = useCase(listOf(sense("a"), sense("b")))

            assertEquals(listOf("a", "b"), repo.confirmed.map { it.translation })
            assertEquals(listOf(SenseId("sense-0"), SenseId("sense-1")), embeddingPort.embedded)
            assertEquals(2, confirmed.size)
        }

    @Test
    fun `an embedding failure is swallowed so the confirm still returns`() =
        runTest {
            val repo = FakeSenseWriteRepository()
            val useCase = ConfirmSensesUseCase(repo, FakeEmbeddingPort(failing = true), noOpAppDiagnostics())

            val confirmed = useCase(listOf(sense("a")))

            assertEquals(listOf("a"), confirmed.map { it.sense.translation })
            assertEquals(listOf("a"), repo.confirmed.map { it.translation })
        }

    @Test
    fun `a failed confirm never embeds`() =
        runTest {
            val embeddingPort = FakeEmbeddingPort()
            val useCase =
                ConfirmSensesUseCase(FakeSenseWriteRepository(confirmFails = true), embeddingPort, noOpAppDiagnostics())

            assertFailsWith<IllegalStateException> { useCase(listOf(sense("a"))) }

            assertTrue(embeddingPort.embedded.isEmpty(), "embedding runs only after the confirm commits")
        }

    @Test
    fun `a slow embedding is abandoned at the eager budget so the confirm does not block`() =
        runTest {
            val repo = FakeSenseWriteRepository()
            val embeddingPort = FakeEmbeddingPort(delayMs = EmbeddingPort.EAGER_EMBED_BUDGET_MS * 10)
            val useCase = ConfirmSensesUseCase(repo, embeddingPort, noOpAppDiagnostics())

            val confirmed = useCase(listOf(sense("a")))

            assertEquals(listOf("a"), repo.confirmed.map { it.translation }, "the durable batch is committed")
            assertEquals(listOf("a"), confirmed.map { it.sense.translation }, "confirm returns the stored rows")
            // The embed overran the budget and was abandoned: without the budget it would
            // have run to completion and recorded the id (so this fails if the budget is dropped).
            assertTrue(embeddingPort.embedded.isEmpty(), "the slow embed is abandoned, left for a later backfill")
        }
}

private fun sense(translation: String): Sense =
    Sense(
        translation = translation,
        contextualApplications = listOf(ContextualApplication(StudiedSentence.parse("[[$translation]] example."))),
    )

private fun stored(
    id: String,
    sense: Sense,
): StoredSense =
    StoredSense(
        id = SenseId(id),
        status = SenseStatus.Confirmed,
        origin = SenseOrigin.Personal,
        sourceRef = null,
        lemmaKey = deriveLemmaKey(sense),
        updatedAtEpochMs = 0L,
        sense = sense,
    )
