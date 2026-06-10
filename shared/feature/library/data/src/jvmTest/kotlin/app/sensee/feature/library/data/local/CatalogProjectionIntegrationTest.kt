package app.sensee.feature.library.data.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.database.DefaultDatabaseTransactionRunner
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
import app.sensee.lexicon.domain.SenseWriteRepository
import app.sensee.lexicon.domain.StoredSense
import app.sensee.lexicon.domain.WriteIntent
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.model.SrsCardState
import app.sensee.srs.engine.factory.SrsCardFactory
import app.sensee.srs.engine.storage.SrsStorage
import app.sensee.srs.fsrs.FsrsParameters
import app.sensee.srs.testKit.InMemorySrsStorage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
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
    private val json = Json { ignoreUnknownKeys = true }

    private object FixedClock : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000L)
    }

    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    private class Fixture(
        failSenseWriteOnCall: Int? = null,
    ) {
        val db =
            SenseeDatabase(
                JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties(), SenseeDatabase.Schema.synchronous()),
            )
        private val provider = FakeDbProvider(db)
        val srs: SrsStorage<FsrsParameters> =
            InMemorySrsStorage(initialParameters = FsrsParameters.defaultV6())
        val senseRepository =
            DefaultSenseRepository(
                databaseProvider = provider,
                transactionRunner = DefaultDatabaseTransactionRunner(provider),
                dispatchers = immediateAppDispatchers(),
                json = Json,
                clock = FixedClock,
                senseIdFactory = DefaultSenseIdFactory(),
                appDiagnostics = noOpAppDiagnostics(),
            )
        private val writeRepository: SenseWriteRepository =
            failSenseWriteOnCall
                ?.let { FailingOnNthUpsert(senseRepository, failOnCall = it) }
                ?: senseRepository
        val source =
            CatalogLocalDataSource(
                databaseProvider = provider,
                transactionRunner = DefaultDatabaseTransactionRunner(provider),
                senseReadRepository = senseRepository,
                senseWriteRepository = writeRepository,
                dispatchers = immediateAppDispatchers(),
                appDiagnostics = noOpAppDiagnostics(),
            )
    }

    // Fails the Nth sense upsert so an earlier card's sense lands inside the ingest
    // transaction first; the thrown failure must then roll that write back together
    // with the deck membership and the subscribe flip (all-or-nothing adopt).
    private class FailingOnNthUpsert(
        private val delegate: SenseWriteRepository,
        private val failOnCall: Int,
    ) : SenseWriteRepository by delegate {
        private var calls = 0

        override suspend fun upsert(
            sense: Sense,
            status: SenseStatus,
            origin: SenseOrigin,
            sourceRef: String?,
            intent: WriteIntent,
        ): StoredSense {
            if (++calls == failOnCall) error("sense store offline")
            return delegate.upsert(sense, status, origin, sourceRef, intent)
        }
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

    private suspend fun Fixture.personalSense(
        translation: String,
        surface: String,
    ): StoredSense =
        senseRepository.upsert(confirmable(translation, surface), SenseStatus.Confirmed, SenseOrigin.Personal)

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
            val cardId = SrsCardId(first.id.value)
            // Practice owns SRS materialization; catalog projection only keeps the
            // stable card id that lets this progress survive re-capture.
            fixture.srs.saveCard(
                SrsCardFactory
                    .newCard(cardId)
                    .copy(state = SrsCardState.Review, reviewCount = 3),
            )

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
    fun `captured deck projection does not materialize SRS on read`() =
        runTest {
            val fixture = Fixture()
            val stored =
                fixture.senseRepository.upsert(
                    confirmable("наткнуться", "come across"),
                    SenseStatus.Confirmed,
                    SenseOrigin.Personal,
                )

            val captured = fixture.source.capturedDeck()

            assertNotNull(captured)
            assertEquals(listOf(stored.id.value), captured.cards.map { it.id.value })
        }

    @Test
    fun `loadCards returns cards in the requested id order`() =
        runTest {
            val fixture = Fixture()
            val a = fixture.personalSense("один", "one")
            val b = fixture.personalSense("два", "two")
            val c = fixture.personalSense("три", "three")

            val cards = fixture.source.selectCards(listOf(c.id.value, a.id.value, b.id.value))

            assertEquals(
                listOf(c.id.value, a.id.value, b.id.value),
                cards.map { it.id.value },
                "selectCards preserves the caller's id order (e.g. due-date ascending)",
            )
        }

    @Test
    fun `loadCards omits ids with no confirmed sense`() =
        runTest {
            val fixture = Fixture()
            val a = fixture.personalSense("один", "one")

            val cards = fixture.source.selectCards(listOf("missing-sense", a.id.value))

            assertEquals(listOf(a.id.value), cards.map { it.id.value })
        }

    @Test
    fun `loadCards de-dups a repeated id`() =
        runTest {
            val fixture = Fixture()
            val a = fixture.personalSense("один", "one")

            val cards = fixture.source.selectCards(listOf(a.id.value, a.id.value))

            assertEquals(listOf(a.id.value), cards.map { it.id.value })
        }

    @Test
    fun `a non-confirmable service card is dropped and membership stays contiguous`() =
        runTest {
            val fixture = Fixture()
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

    @Test
    fun `a failure mid-ingest rolls back senses, membership and the subscription`() =
        runTest {
            val fixture = Fixture(failSenseWriteOnCall = 2)
            val deck =
                json.decodeFromString<DeckDto>(
                    CatalogMockFixtures().fixtures.getValue("practice/decks/phrasal-verbs-come"),
                )

            assertFailsWith<IllegalStateException> { fixture.source.ingestDeck(deck, subscribe = true) }

            assertTrue(
                fixture.senseRepository.listByStatus(SenseStatus.Confirmed).isEmpty(),
                "an already-written sense rolls back with the failed transaction — no orphan rows",
            )
            assertTrue(
                fixture.db.catalogEntityQueries
                    .selectMembershipByDeck(deck.id)
                    .awaitAsList()
                    .isEmpty(),
                "no membership is left behind",
            )
            assertNull(
                fixture.db.catalogEntityQueries
                    .selectDeckById(deck.id)
                    .awaitAsOneOrNull(),
                "the deck meta and subscribe flip roll back too",
            )
        }
}
