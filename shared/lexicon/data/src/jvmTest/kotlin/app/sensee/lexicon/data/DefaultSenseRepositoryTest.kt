package app.sensee.lexicon.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
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
import app.sensee.lexicon.domain.WriteIntent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Repository coverage over a real in-memory database: identity is stable across
 * edits and re-captures, the write intents behave as specified, and the
 * origin-scoped resolve keeps Personal and Service material apart.
 */
class DefaultSenseRepositoryTest {
    private class MutableClock(
        var nowMs: Long = 1_000L,
    ) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(nowMs)
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

    private fun newRepo(clock: Clock = MutableClock()): DefaultSenseRepository {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY,
                Properties(),
                SenseeDatabase.Schema.synchronous(),
            )
        return DefaultSenseRepository(
            databaseProvider = FakeDbProvider(SenseeDatabase(driver)),
            dispatchers = immediateAppDispatchers(),
            json = Json,
            clock = clock,
            senseIdFactory = SequentialSenseIdFactory(),
            appDiagnostics = noOpAppDiagnostics(),
        )
    }

    private fun sense(
        translation: String,
        surface: String = "come across",
    ): Sense =
        Sense(
            translation = translation,
            surfaceForm = SurfaceForm.parse(surface),
            contextualApplications = listOf(ContextualApplication(StudiedSentence.parse("I [[$surface]] it."))),
        )

    @Test
    fun `confirmAll persists every sense and reuses ids on re-confirm`() =
        runTest {
            val repo = newRepo()
            val first = repo.confirmAll(listOf(sense("наткнуться"), sense("задержка", "latency")))

            assertEquals(2, repo.listByStatus(SenseStatus.Confirmed).size)
            val second = repo.confirmAll(listOf(sense("наткнуться")))
            assertEquals(first.first().id, second.single().id, "re-confirm reuses the sense_id")
            assertEquals(2, repo.listByStatus(SenseStatus.Confirmed).size, "no duplicate row")
        }

    @Test
    fun `confirmAll writes nothing when any sense fails the confirm-gate`() =
        runTest {
            val repo = newRepo()
            val bad = Sense(translation = "плохой", surfaceForm = SurfaceForm.parse("bad"))

            assertFailsWith<IllegalArgumentException> {
                repo.confirmAll(listOf(sense("наткнуться"), bad))
            }
            assertEquals(0, repo.listByStatus(SenseStatus.Confirmed).size, "a rejected batch persists nothing")
        }

    @Test
    fun `confirmAll collapses an in-batch content-key duplicate to one row`() =
        runTest {
            val repo = newRepo()
            val stored = repo.confirmAll(listOf(sense("наткнуться"), sense("наткнуться")))

            assertEquals(1, repo.listByStatus(SenseStatus.Confirmed).size, "an in-batch duplicate is one row")
            assertEquals(stored.first().id, stored[1].id, "both senses map to the same sense_id")
        }

    @Test
    fun `resolveOrMint reuses the sense_id for an exact content match`() =
        runTest {
            val repo = newRepo()

            val first = repo.upsert(sense("наткнуться"), SenseStatus.Confirmed, SenseOrigin.Personal)
            val second = repo.upsert(sense("наткнуться"), SenseStatus.Confirmed, SenseOrigin.Personal)

            assertEquals(first.id, second.id)
            assertEquals(1, repo.listByStatus(SenseStatus.Confirmed).size)
        }

    @Test
    fun `UpdateExisting preserves the sense_id and replaces the content`() =
        runTest {
            val repo = newRepo()

            val stored = repo.upsert(sense("наткнуться"), SenseStatus.Confirmed, SenseOrigin.Personal)
            val edited =
                repo.upsert(
                    sense("встретить случайно"),
                    SenseStatus.Confirmed,
                    SenseOrigin.Personal,
                    intent = WriteIntent.UpdateExisting(stored.id),
                )

            assertEquals(stored.id, edited.id)
            assertEquals("встретить случайно", repo.getById(stored.id)?.sense?.translation)
        }

    @Test
    fun `ForceMint creates a separate row for the same content`() =
        runTest {
            val repo = newRepo()

            val first = repo.upsert(sense("наткнуться"), SenseStatus.Confirmed, SenseOrigin.Personal)
            val forced =
                repo.upsert(
                    sense("наткнуться"),
                    SenseStatus.Confirmed,
                    SenseOrigin.Personal,
                    intent = WriteIntent.ForceMint,
                )

            assertNotEquals(first.id, forced.id)
            assertEquals(2, repo.listByStatus(SenseStatus.Confirmed).size)
        }

    @Test
    fun `resolveOrMint tie-breaks to the most recently updated duplicate`() =
        runTest {
            val clock = MutableClock(1_000L)
            val repo = newRepo(clock)

            val older =
                repo.upsert(
                    sense("наткнуться"),
                    SenseStatus.Confirmed,
                    SenseOrigin.Personal,
                    intent = WriteIntent.ForceMint,
                )
            clock.nowMs = 2_000L
            val newer =
                repo.upsert(
                    sense("наткнуться"),
                    SenseStatus.Confirmed,
                    SenseOrigin.Personal,
                    intent = WriteIntent.ForceMint,
                )
            clock.nowMs = 3_000L
            val resolved = repo.upsert(sense("наткнуться"), SenseStatus.Confirmed, SenseOrigin.Personal)

            assertEquals(newer.id, resolved.id)
            assertNotEquals(older.id, resolved.id)
        }

    @Test
    fun `a Personal sense and a Service ingest of the same content stay separate rows`() =
        runTest {
            val repo = newRepo()

            val personal = repo.upsert(sense("наткнуться"), SenseStatus.Confirmed, SenseOrigin.Personal)
            val service =
                repo.upsert(sense("наткнуться"), SenseStatus.Confirmed, SenseOrigin.Service, sourceRef = "deck1-card3")

            assertNotEquals(personal.id, service.id)
            assertEquals(SenseOrigin.Personal, repo.getById(personal.id)?.origin)
            assertEquals(2, repo.listByStatus(SenseStatus.Confirmed).size)
        }

    @Test
    fun `a Service re-sync reuses the sense_id by source_ref and overwrites content`() =
        runTest {
            val repo = newRepo()

            val first =
                repo.upsert(sense("наткнуться"), SenseStatus.Confirmed, SenseOrigin.Service, sourceRef = "deck1-card3")
            val resync =
                repo.upsert(sense("встретить"), SenseStatus.Confirmed, SenseOrigin.Service, sourceRef = "deck1-card3")

            assertEquals(first.id, resync.id)
            assertEquals("встретить", repo.getById(first.id)?.sense?.translation)
            assertEquals(1, repo.listByStatus(SenseStatus.Confirmed).size)
        }

    @Test
    fun `a Service sense without a source_ref is rejected`() =
        runTest {
            val repo = newRepo()

            assertFailsWith<IllegalArgumentException> {
                repo.upsert(sense("x"), SenseStatus.Confirmed, SenseOrigin.Service, sourceRef = null)
            }
        }

    @Test
    fun `a Confirmed write enforces the confirm-gate but a Draft does not`() =
        runTest {
            val repo = newRepo()

            assertFailsWith<IllegalArgumentException> {
                repo.upsert(Sense(translation = ""), SenseStatus.Confirmed, SenseOrigin.Personal)
            }

            val draft =
                repo.upsert(
                    Sense(translation = "", surfaceForm = SurfaceForm.parse("come across")),
                    SenseStatus.Draft,
                    SenseOrigin.Personal,
                )
            assertEquals(SenseStatus.Draft, repo.getById(draft.id)?.status)
        }

    @Test
    fun `two blank drafts of the same word stay separate authoring seats`() =
        runTest {
            val repo = newRepo()
            val bare = { Sense(translation = "", surfaceForm = SurfaceForm.parse("come across")) }

            val first = repo.upsert(bare(), SenseStatus.Draft, SenseOrigin.Personal)
            val second = repo.upsert(bare(), SenseStatus.Draft, SenseOrigin.Personal)

            assertNotEquals(first.id, second.id)
        }

    @Test
    fun `getById returns null for an unknown id`() =
        runTest {
            assertNull(newRepo().getById(SenseId("missing")))
        }
}
