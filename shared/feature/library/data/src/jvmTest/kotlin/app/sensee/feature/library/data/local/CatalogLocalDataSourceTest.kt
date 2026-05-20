package app.sensee.feature.library.data.local

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.library.data.remote.DeckSummaryDto
import app.sensee.feature.library.domain.CatalogOrigin
import app.sensee.srs.fsrs.FsrsParameters
import app.sensee.srs.testKit.InMemorySrsStorage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Repository-layer coverage over a real in-memory database: adoption is a
 * provenance flip (not a copy), a Service re-sync preserves adoption while
 * refreshing the advertised card count, and un-adopt returns the deck to a
 * passive Service cache row.
 */
class CatalogLocalDataSourceTest {
    private object FixedClock : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000L)
    }

    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    private fun newSource(): CatalogLocalDataSource {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY,
                Properties(),
                SenseeDatabase.Schema.synchronous(),
            )
        return CatalogLocalDataSource(
            databaseProvider = FakeDbProvider(SenseeDatabase(driver)),
            srsStorage = InMemorySrsStorage(initialParameters = FsrsParameters.defaultV6()),
            dispatchers = immediateAppDispatchers(),
            json = Json,
            clock = FixedClock,
        )
    }

    private fun summary(
        id: String,
        cards: Int,
    ) = DeckSummaryDto(id = id, title = "Title $id", description = "Desc", cardCount = cards)

    @Test
    fun `a synced service deck is passive cache with its advertised card count`() =
        runTest {
            val source = newSource()
            source.upsertDeckSummaries(listOf(summary("d1", cards = 12)))

            val deck = source.selectDecks().single()
            assertEquals(CatalogOrigin.Service, deck.origin, "not adopted yet")
            assertEquals(12, deck.cardCount, "advertised count survives without local card links")
            assertEquals(emptyList(), source.selectOwnedDecks(), "a cache deck is not owned")
        }

    @Test
    fun `adopt flips provenance to Personal and un-adopt flips it back`() =
        runTest {
            val source = newSource()
            source.upsertDeckSummaries(listOf(summary("d1", cards = 3)))

            source.markDeckAdopted("d1")
            assertEquals(CatalogOrigin.Personal, source.selectDecks().single().origin)
            assertEquals(listOf("d1"), source.selectOwnedDecks().map { it.id.value })

            source.clearDeckAdopted("d1")
            assertEquals(CatalogOrigin.Service, source.selectDecks().single().origin)
            assertEquals(emptyList(), source.selectOwnedDecks())

            source.markDeckAdopted("d1")
            assertEquals(listOf("d1"), source.selectOwnedDecks().map { it.id.value })
        }

    @Test
    fun `a service re-sync preserves adoption and refreshes the card count`() =
        runTest {
            val source = newSource()
            source.upsertDeckSummaries(listOf(summary("d1", cards = 5)))
            source.markDeckAdopted("d1")

            source.upsertDeckSummaries(listOf(summary("d1", cards = 9)))

            val deck = source.selectDecks().single()
            assertEquals(CatalogOrigin.Personal, deck.origin, "adoption survives the re-sync")
            assertEquals(9, deck.cardCount, "advertised count is refreshed by the re-sync")
        }
}
