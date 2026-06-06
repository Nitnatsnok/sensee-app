package app.sensee.feature.library.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.database.DefaultDatabaseTransactionRunner
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.library.domain.CardId
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.lexicon.data.DefaultSenseIdFactory
import app.sensee.lexicon.data.DefaultSenseRepository
import app.sensee.lexicon.domain.ContextualApplication
import app.sensee.lexicon.domain.EmbeddingPort
import app.sensee.lexicon.domain.EmbeddingVector
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.SenseId
import app.sensee.lexicon.domain.SenseOrigin
import app.sensee.lexicon.domain.SenseStatus
import app.sensee.lexicon.domain.SimilarSense
import app.sensee.lexicon.domain.StoredSense
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.fsrs.FsrsParameters
import app.sensee.srs.testKit.InMemorySrsStorage
import app.sensee.srs.testKit.SrsTestCards
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * `claim` takes a detached Personal copy of a Service sense: a fresh sense_id, the
 * content copied, and SRS cloned onto the new id for the sense and its form cards —
 * all while the source sense and its SRS are left untouched.
 */
class ClaimTest {
    private object FixedClock : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000L)
    }

    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    // Records the senses it was asked to embed; [failing] exercises the best-effort
    // path (the caller must swallow the error).
    private class RecordingEmbeddingPort(
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

    private class Fixture(
        val embedding: RecordingEmbeddingPort = RecordingEmbeddingPort(),
    ) {
        private val db =
            SenseeDatabase(
                JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties(), SenseeDatabase.Schema.synchronous()),
            )
        private val provider = FakeDbProvider(db)
        val srs = InMemorySrsStorage(initialParameters = FsrsParameters.defaultV6())
        val senseRepository =
            DefaultSenseRepository(
                provider,
                immediateAppDispatchers(),
                Json,
                FixedClock,
                DefaultSenseIdFactory(),
                noOpAppDiagnostics(),
            )
        val claim =
            DefaultClaimRepository(
                senseRepository,
                senseRepository,
                embedding,
                srs,
                DefaultDatabaseTransactionRunner(provider),
                noOpAppDiagnostics(),
            )

        suspend fun upsertService(sourceRef: String): StoredSense =
            senseRepository.upsert(
                sense =
                    Sense(
                        translation = "бросать",
                        surfaceForm = SurfaceForm.parse("throw"),
                        contextualApplications =
                            listOf(ContextualApplication(StudiedSentence.parse("I [[throw]] it."))),
                    ),
                status = SenseStatus.Confirmed,
                origin = SenseOrigin.Service,
                sourceRef = sourceRef,
            )
    }

    @Test
    fun `claim mints a detached personal copy and clones SRS for the sense and its forms`() =
        runTest {
            val fixture = Fixture()
            val service =
                fixture.senseRepository.upsert(
                    sense =
                        Sense(
                            translation = "бросать",
                            surfaceForm = SurfaceForm.parse("throw"),
                            contextualApplications =
                                listOf(
                                    ContextualApplication(StudiedSentence.parse("I [[throw]] it.")),
                                ),
                            irregularForms = IrregularForms("throw", "threw", "thrown"),
                        ),
                    status = SenseStatus.Confirmed,
                    origin = SenseOrigin.Service,
                    sourceRef = "card-x",
                )
            val formSuffix = ":form:${GrammarForm.PastTense.id}"
            fixture.srs.saveCard(SrsTestCards.newCard(service.id.value).copy(reviewCount = 7))
            fixture.srs.saveCard(SrsTestCards.newCard("${service.id.value}$formSuffix").copy(reviewCount = 3))

            val claimed = fixture.claim.claim(CardId(service.id.value))

            assertNotEquals(service.id.value, claimed.value, "claim mints a fresh Personal sense_id")
            val copy = fixture.senseRepository.getById(SenseId(claimed.value))
            assertEquals(SenseOrigin.Personal, copy?.origin, "the copy is Personal")
            assertEquals("бросать", copy?.sense?.translation, "content is copied")

            assertEquals(
                7,
                fixture.srs.getCard(SrsCardId(claimed.value))?.reviewCount,
                "base SRS cloned onto the new id",
            )
            assertEquals(
                3,
                fixture.srs.getCard(SrsCardId("${claimed.value}$formSuffix"))?.reviewCount,
                "form SRS cloned onto the new id",
            )

            assertEquals(7, fixture.srs.getCard(SrsCardId(service.id.value))?.reviewCount, "source SRS untouched")
            assertEquals(
                SenseOrigin.Service,
                fixture.senseRepository.getById(service.id)?.origin,
                "source sense untouched",
            )
        }

    @Test
    fun `claim embeds the detached copy`() =
        runTest {
            val fixture = Fixture()
            val service = fixture.upsertService("card-x")

            val claimed = fixture.claim.claim(CardId(service.id.value))

            assertEquals(
                listOf(SenseId(claimed.value)),
                fixture.embedding.embedded,
                "the detached copy is embedded for similarity coverage, not the source",
            )
        }

    @Test
    fun `a claim still succeeds when embedding fails`() =
        runTest {
            val fixture = Fixture(RecordingEmbeddingPort(failing = true))
            val service = fixture.upsertService("card-x")

            val claimed = fixture.claim.claim(CardId(service.id.value))

            assertEquals(
                SenseOrigin.Personal,
                fixture.senseRepository.getById(SenseId(claimed.value))?.origin,
                "the copy is committed even though embedding failed (best-effort, post-commit)",
            )
        }

    @Test
    fun `a slow embedding does not block the claim past the eager budget`() =
        runTest {
            val fixture = Fixture(RecordingEmbeddingPort(delayMs = EmbeddingPort.EAGER_EMBED_BUDGET_MS * 10))
            val service = fixture.upsertService("card-x")

            val claimed = fixture.claim.claim(CardId(service.id.value))

            assertEquals(
                SenseOrigin.Personal,
                fixture.senseRepository.getById(SenseId(claimed.value))?.origin,
                "the copy is committed even though the embed overran its budget",
            )
            // The embed overran the budget and was abandoned: without the budget it would
            // have run to completion and recorded the id (so this fails if the budget is dropped).
            assertTrue(fixture.embedding.embedded.isEmpty(), "the slow embed is abandoned, left for a later backfill")
        }
}
