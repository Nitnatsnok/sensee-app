package app.sensee.lexicon.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.ai.core.contract.AiEmbeddingClient
import app.sensee.ai.core.contract.Embedding
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.lexicon.domain.ContextualApplication
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.SenseId
import app.sensee.lexicon.domain.SenseOrigin
import app.sensee.lexicon.domain.SenseStatus
import app.sensee.lexicon.domain.StoredSense
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The embedding port over a real in-memory sense store with a fake vector
 * provider: a saved sense gets a normalized vector row, a provider miss leaves it
 * unembedded without throwing, and `findSimilar` ranks above-threshold senses
 * strongest-first while excluding the query, mismatched `(model, dim)`, and
 * embeddings whose sense row is gone.
 */
class DefaultEmbeddingPortTest {
    private class FixedClock : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000L)
    }

    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    private class SequentialSenseIdFactory : SenseIdFactory {
        private var counter = 0

        override fun mintPersonal(): SenseId = SenseId("p-${++counter}")

        override fun forServiceSource(sourceRef: String): SenseId = SenseId("svc-$sourceRef")
    }

    // Returns the vector whose token appears in the embedding text, else null
    // (the offline / no-coverage state). An empty map is the offline client.
    private class FakeEmbeddingClient(
        private val byToken: Map<String, FloatArray>,
        private val model: String = MODEL,
    ) : AiEmbeddingClient {
        override suspend fun embed(text: String): Embedding? =
            byToken.entries.firstOrNull { text.contains(it.key) }?.let { Embedding(it.value, model) }
    }

    private class Fixture(
        val db: SenseeDatabase,
        val repo: DefaultSenseRepository,
        val port: DefaultEmbeddingPort,
    )

    private fun fixture(client: AiEmbeddingClient): Fixture {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY,
                Properties(),
                SenseeDatabase.Schema.synchronous(),
            )
        val db = SenseeDatabase(driver)
        val provider = FakeDbProvider(db)
        val repo =
            DefaultSenseRepository(
                databaseProvider = provider,
                dispatchers = immediateAppDispatchers(),
                json = Json,
                clock = FixedClock(),
                senseIdFactory = SequentialSenseIdFactory(),
                appDiagnostics = noOpAppDiagnostics(),
            )
        return Fixture(db, repo, DefaultEmbeddingPort(provider, repo, client, noOpAppDiagnostics()))
    }

    private fun sense(
        translation: String,
        surface: String,
    ): Sense =
        Sense(
            translation = translation,
            surfaceForm = SurfaceForm.parse(surface),
            contextualApplications = listOf(ContextualApplication(StudiedSentence.parse("I [[$surface]] it."))),
        )

    private suspend fun DefaultSenseRepository.put(
        translation: String,
        surface: String,
    ): StoredSense = upsert(sense(translation, surface), SenseStatus.Confirmed, SenseOrigin.Personal)

    @Test
    fun `embed persists a normalized vector for the sense`() =
        runTest {
            val fixture = fixture(FakeEmbeddingClient(mapOf("mid" to MID)))
            val stored = fixture.repo.put("mid", "alpha")

            val vector = fixture.port.embed(stored)

            assertEquals(MODEL, vector?.modelRef)
            val row =
                fixture.db.senseEmbeddingQueries
                    .selectById(stored.id.value)
                    .awaitAsOneOrNull()
            assertEquals(stored.id.value, row?.sense_id)
            assertEquals(3L, row?.dim)
            assertEquals(MODEL, row?.model_ref)
            val decoded = row!!.vector.toFloatVector()
            val length = sqrt(decoded.fold(0f) { acc, value -> acc + value * value })
            assertTrue(abs(1f - length) < 1e-5f, "stored vector should be unit length, was $length")
        }

    @Test
    fun `embed without a provider vector leaves the sense unembedded`() =
        runTest {
            val fixture = fixture(FakeEmbeddingClient(emptyMap()))
            val stored = fixture.repo.put("mid", "alpha")

            assertNull(fixture.port.embed(stored))
            assertNull(
                fixture.db.senseEmbeddingQueries
                    .selectById(stored.id.value)
                    .awaitAsOneOrNull(),
            )
        }

    @Test
    fun `findSimilar returns senses above the threshold, strongest first`() =
        runTest {
            val fixture =
                fixture(FakeEmbeddingClient(mapOf("base" to BASE, "strong" to STRONG, "mid" to MID, "weak" to WEAK)))
            val base = fixture.repo.put("base", "a")
            val strong = fixture.repo.put("strong", "b")
            val mid = fixture.repo.put("mid", "c")
            val weak = fixture.repo.put("weak", "d")
            val query = fixture.port.embed(base)!!
            fixture.port.embed(strong)
            fixture.port.embed(mid)
            fixture.port.embed(weak)

            val similar = fixture.port.findSimilar(query, excluding = base.id)

            // strong (~0.99) and mid (~0.96) clear 0.85; weak (~0.80) does not; base is excluded.
            assertEquals(listOf(strong.id, mid.id), similar.map { it.sense.id })
            assertTrue(similar[0].score >= similar[1].score)
        }

    @Test
    fun `findSimilar ignores vectors from a different model or dimension`() =
        runTest {
            val fixture = fixture(FakeEmbeddingClient(mapOf("base" to BASE, "mid" to MID)))
            val base = fixture.repo.put("base", "a")
            val mid = fixture.repo.put("mid", "b")
            val query = fixture.port.embed(base)!!
            fixture.port.embed(mid)
            // Same direction as base but a different model — must be gated out.
            val otherModel = fixture.repo.put("other", "c")
            fixture.db.senseEmbeddingQueries.upsert(
                otherModel.id.value,
                BASE.l2Normalized()!!.toVectorBytes(),
                "other-model",
                3L,
            )
            // Same model but a different dimension — also gated out.
            val otherDim = fixture.repo.put("dim", "d")
            fixture.db.senseEmbeddingQueries.upsert(otherDim.id.value, floatArrayOf(1f, 0f).toVectorBytes(), MODEL, 2L)

            val similar = fixture.port.findSimilar(query, excluding = base.id)

            assertEquals(listOf(mid.id), similar.map { it.sense.id })
        }

    @Test
    fun `findSimilar skips an embedding whose sense row is gone`() =
        runTest {
            val fixture = fixture(FakeEmbeddingClient(mapOf("base" to BASE, "mid" to MID)))
            val base = fixture.repo.put("base", "a")
            val mid = fixture.repo.put("mid", "b")
            val query = fixture.port.embed(base)!!
            fixture.port.embed(mid)
            // Drop the sense row but leave its embedding (cascade may be disabled).
            fixture.db.senseQueries.deleteById(mid.id.value)

            assertTrue(fixture.port.findSimilar(query, excluding = base.id).isEmpty())
        }

    @Test
    fun `the similarity threshold is inclusive at the exact score`() =
        runTest {
            val fixture = fixture(FakeEmbeddingClient(mapOf("base" to BASE, "mid" to MID)))
            val base = fixture.repo.put("base", "a")
            val mid = fixture.repo.put("mid", "b")
            val query = fixture.port.embed(base)!!
            fixture.port.embed(mid)

            // The candidate's own score, measured with no threshold, is the boundary.
            val score =
                fixture.port
                    .findSimilar(query, excluding = base.id, threshold = 0f)
                    .single()
                    .score

            // At the score it is included (>= , not >); just above it is excluded.
            assertEquals(
                listOf(mid.id),
                fixture.port.findSimilar(query, excluding = base.id, threshold = score).map { it.sense.id },
            )
            assertTrue(fixture.port.findSimilar(query, excluding = base.id, threshold = score + 1e-4f).isEmpty())
        }

    @Test
    fun `re-embedding a sense replaces its single vector row`() =
        runTest {
            val client =
                object : AiEmbeddingClient {
                    private val responses =
                        ArrayDeque(
                            listOf(
                                Embedding(floatArrayOf(1f, 0f, 0f), "model-v1"),
                                Embedding(floatArrayOf(0f, 1f, 0f), "model-v2"),
                            ),
                        )

                    override suspend fun embed(text: String): Embedding = responses.removeFirst()
                }
            val fixture = fixture(client)
            val stored = fixture.repo.put("base", "a")

            fixture.port.embed(stored)
            fixture.port.embed(stored)

            // One row per sense_id (PK): the re-embed replaced the old model's row.
            val row =
                fixture.db.senseEmbeddingQueries
                    .selectById(stored.id.value)
                    .awaitAsOneOrNull()
            assertEquals("model-v2", row?.model_ref)
            assertTrue(
                fixture.db.senseEmbeddingQueries
                    .selectByModel("model-v1", 3L)
                    .awaitAsList()
                    .isEmpty(),
            )
        }

    private companion object {
        const val MODEL = "test-model"
        val BASE = floatArrayOf(1f, 0f, 0f)
        val STRONG = floatArrayOf(0.99f, 0.141067f, 0f) // cosine ~0.99 with BASE
        val MID = floatArrayOf(0.96f, 0.28f, 0f) // cosine ~0.96 with BASE
        val WEAK = floatArrayOf(0.8f, 0.6f, 0f) // cosine ~0.80 with BASE (below threshold)
    }
}
