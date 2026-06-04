package app.sensee.feature.library.data.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.library.data.remote.CardDto
import app.sensee.feature.library.data.remote.CatalogMockFixtures
import app.sensee.feature.library.data.remote.DeckDto
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.lexicon.data.DefaultSenseIdFactory
import app.sensee.lexicon.data.DefaultSenseRepository
import app.sensee.lexicon.domain.ContextualApplication
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.SenseOrigin
import app.sensee.lexicon.domain.SenseStatus
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.model.SrsCardState
import app.sensee.srs.fsrs.FsrsParameters
import app.sensee.srs.testKit.InMemorySrsStorage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The flip's headline invariant, end-to-end through the REAL sense store: a
 * re-capture of the same content resolves to the same sense_id, so the captured
 * card id (the SRS anchor) is stable and accumulated SRS is not orphaned. Uses the
 * real [DefaultSenseRepository] (not a minting fake) so the resolve-or-mint path is
 * actually exercised.
 */
class CatalogProjectionIntegrationTest {
    private object FixedClock : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000L)
    }

    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    private class Fixture {
        val db =
            SenseeDatabase(
                JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties(), SenseeDatabase.Schema.synchronous()),
            )
        private val provider = FakeDbProvider(db)
        val srs = InMemorySrsStorage(initialParameters = FsrsParameters.defaultV6())
        val senseRepository =
            DefaultSenseRepository(
                databaseProvider = provider,
                dispatchers = immediateAppDispatchers(),
                json = Json,
                clock = FixedClock,
                senseIdFactory = DefaultSenseIdFactory(),
                appDiagnostics = noOpAppDiagnostics(),
            )
        val source =
            CatalogLocalDataSource(
                databaseProvider = provider,
                senseReadRepository = senseRepository,
                senseWriteRepository = senseRepository,
                srsStorage = srs,
                dispatchers = immediateAppDispatchers(),
                appDiagnostics = noOpAppDiagnostics(),
            )
    }

    private fun confirmable(
        translation: String,
        surface: String,
    ): Sense =
        Sense(
            translation = translation,
            surfaceForm = SurfaceForm.parse(surface),
            contextualApplications = listOf(ContextualApplication(StudiedSentence.parse("I [[$surface]] it."))),
        )

    @Test
    fun `re-capturing a sense keeps its captured card id and accumulated SRS`() =
        runTest {
            val fixture = Fixture()

            val first =
                fixture.senseRepository.upsert(
                    confirmable("наткнуться", "come across"),
                    SenseStatus.Confirmed,
                    SenseOrigin.Personal,
                )
            // Project once → materializes the New SRS row, then simulate progress.
            fixture.source.capturedDeck()
            val cardId = SrsCardId(first.id.value)
            fixture.srs.saveCard(fixture.srs.getCard(cardId)!!.copy(state = SrsCardState.Review, reviewCount = 3))

            // A re-capture of identical content must reuse the sense_id (ResolveOrMint).
            val second =
                fixture.senseRepository.upsert(
                    confirmable("наткнуться", "come across"),
                    SenseStatus.Confirmed,
                    SenseOrigin.Personal,
                )
            assertEquals(first.id, second.id, "re-capture reuses the sense_id")

            val captured = fixture.source.capturedDeck()
            assertNotNull(captured)
            assertEquals(
                listOf(first.id.value),
                captured.cards.map { it.id.value },
                "the captured card id is the (stable) sense_id, not a new one",
            )
            // SRS on the reused sense_id survives the re-capture and the re-projection.
            val afterRecapture = fixture.srs.getCard(cardId)
            assertEquals(3, afterRecapture?.reviewCount, "re-capture must not orphan or reset SRS")
            assertEquals(SrsCardState.Review, afterRecapture?.state)
        }

    @Test
    fun `a non-confirmable service card is dropped and membership stays contiguous`() =
        runTest {
            val fixture = Fixture()
            val json = Json { ignoreUnknownKeys = true }
            val good =
                json.decodeFromString<DeckDto>(
                    CatalogMockFixtures().fixtures.getValue("practice/decks/phrasal-verbs-come"),
                )
            // A card whose sense has no example → fails the confirm-gate.
            val bad =
                json.decodeFromString(
                    CardDto.serializer(),
                    """{"id":"card-bad","lemma_id":"lemma-bad","sense":{"translation":"плохой","surfaceForm":"bad","unitType":"adjective"}}""",
                )

            fixture.source.ingestDeck(good.copy(cards = good.cards + bad))

            assertNull(
                fixture.senseRepository.getById(DefaultSenseIdFactory().forServiceSource("card-bad")),
                "a card with no confirmable sense is dropped, not ingested",
            )
            assertNotNull(
                fixture.senseRepository.getById(DefaultSenseIdFactory().forServiceSource("card-come")),
                "a confirmable card is ingested",
            )
            val positions =
                fixture.db.catalogEntityQueries
                    .selectMembershipByDeck(good.id)
                    .awaitAsList()
                    .map { it.position }
            assertEquals(
                (0 until positions.size).map { it.toLong() },
                positions,
                "dropping a card must not leave a gap in membership positions",
            )
        }
}
