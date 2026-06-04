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
import app.sensee.lexicon.domain.SenseQuery
import app.sensee.lexicon.domain.SenseStatus
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Search coverage over a real in-memory database: a free-text query matches the
 * stored senses (surface / translation / lemma), ranks exact/prefix above
 * substrings, breaks ties by recency, honours the status filter and limit, and
 * ignores argument-slot placeholder names in the surface form.
 */
class SenseSearchTest {
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
        surface: String,
        baseLemma: String? = null,
        headLemma: String? = null,
    ): Sense =
        Sense(
            translation = translation,
            surfaceForm = SurfaceForm.parse(surface),
            baseLemma = baseLemma,
            headLemma = headLemma,
            contextualApplications = listOf(ContextualApplication(StudiedSentence.parse("I [[$surface]] it."))),
        )

    private suspend fun DefaultSenseRepository.surfacesFor(query: SenseQuery): List<String?> =
        search(query).map { it.sense.surfaceForm?.display() }

    @Test
    fun `search matches surface form and translation case-insensitively`() =
        runTest {
            val repo = newRepo()
            repo.upsert(sense("наткнуться", "come across"), SenseStatus.Confirmed, SenseOrigin.Personal)
            repo.upsert(sense("задержка", "latency"), SenseStatus.Confirmed, SenseOrigin.Personal)

            assertEquals(listOf("come across"), repo.surfacesFor(SenseQuery(text = "COME")))
            assertEquals(listOf("latency"), repo.surfacesFor(SenseQuery(text = "задерж")))
        }

    @Test
    fun `search ranks exact and prefix matches above substrings`() =
        runTest {
            val repo = newRepo()
            repo.upsert(sense("a", "come"), SenseStatus.Confirmed, SenseOrigin.Personal)
            repo.upsert(sense("b", "come across"), SenseStatus.Confirmed, SenseOrigin.Personal)
            repo.upsert(sense("c", "overcome"), SenseStatus.Confirmed, SenseOrigin.Personal)

            assertEquals(
                listOf("come", "come across", "overcome"),
                repo.surfacesFor(SenseQuery(text = "come")),
            )
        }

    @Test
    fun `search breaks ties by recency, newest first`() =
        runTest {
            val clock = MutableClock(nowMs = 1_000L)
            val repo = newRepo(clock)
            repo.upsert(sense("a", "come across"), SenseStatus.Confirmed, SenseOrigin.Personal)
            clock.nowMs = 2_000L
            repo.upsert(sense("b", "come along"), SenseStatus.Confirmed, SenseOrigin.Personal)

            // Both are prefix matches for "come"; the more recently updated one ranks first.
            assertEquals(
                listOf("come along", "come across"),
                repo.surfacesFor(SenseQuery(text = "come")),
            )
        }

    @Test
    fun `search finds a sense by translation when it has no surface form`() =
        runTest {
            val repo = newRepo()
            repo.upsert(
                Sense(
                    translation = "наткнуться",
                    contextualApplications = listOf(ContextualApplication(StudiedSentence.parse("I found it."))),
                ),
                SenseStatus.Confirmed,
                SenseOrigin.Personal,
            )

            assertEquals(1, repo.search(SenseQuery(text = "наткну")).size)
        }

    @Test
    fun `search ignores argument-slot placeholder names in the surface form`() =
        runTest {
            val repo = newRepo()
            repo.upsert(
                sense("наткнуться", "come across <something>", baseLemma = "come across", headLemma = "come across"),
                SenseStatus.Confirmed,
                SenseOrigin.Personal,
            )

            assertEquals(1, repo.search(SenseQuery(text = "come across")).size)
            assertEquals(0, repo.search(SenseQuery(text = "something")).size)
        }

    @Test
    fun `search narrows by status`() =
        runTest {
            val repo = newRepo()
            repo.upsert(sense("наткнуться", "come across"), SenseStatus.Confirmed, SenseOrigin.Personal)
            repo.upsert(
                Sense(translation = "", surfaceForm = SurfaceForm.parse("come up")),
                SenseStatus.Draft,
                SenseOrigin.Personal,
            )

            assertEquals(
                listOf("come across"),
                repo.surfacesFor(SenseQuery(text = "come", status = SenseStatus.Confirmed)),
            )
        }

    @Test
    fun `a zero limit returns nothing and a negative limit is rejected`() =
        runTest {
            val repo = newRepo()
            repo.upsert(sense("наткнуться", "come across"), SenseStatus.Confirmed, SenseOrigin.Personal)

            assertEquals(0, repo.search(SenseQuery(text = "come", limit = 0)).size)
            assertFailsWith<IllegalArgumentException> { SenseQuery(text = "come", limit = -1) }
        }

    @Test
    fun `a blank query returns nothing`() =
        runTest {
            val repo = newRepo()
            repo.upsert(sense("наткнуться", "come across"), SenseStatus.Confirmed, SenseOrigin.Personal)

            assertEquals(0, repo.search(SenseQuery(text = "   ")).size)
        }
}
