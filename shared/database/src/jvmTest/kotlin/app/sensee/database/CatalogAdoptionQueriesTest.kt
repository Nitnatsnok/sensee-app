package app.sensee.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CatalogAdoptionQueriesTest {
    @Test
    fun `a service re-sync preserves the adopted flag while refreshing metadata`() =
        runTest {
            val db = freshInMemoryDatabase()
            db.catalogEntityQueries.insertDeckIfAbsent("d1", "Title", "Desc", card_count = 5)
            db.catalogEntityQueries.updateDeckMeta("Title", "Desc", card_count = 5, id = "d1")
            db.catalogEntityQueries.markDeckAdopted(adopted_at_epoch_ms = 1_000L, id = "d1")

            db.catalogEntityQueries.insertDeckIfAbsent("d1", "New title", "New desc", card_count = 7)
            db.catalogEntityQueries.updateDeckMeta("New title", "New desc", card_count = 7, id = "d1")

            val row =
                db.catalogEntityQueries
                    .selectAllDecks()
                    .awaitAsList()
                    .single()
            assertEquals("New title", row.title, "metadata is refreshed by the sync")
            assertEquals(1_000L, row.adopted_at_epoch_ms, "adoption survives a Service re-sync")
            assertEquals(7L, row.card_count, "the advertised card count is refreshed by the sync")
        }

    @Test
    fun `a suggested deck reports its advertised card count without local card links`() =
        runTest {
            val db = freshInMemoryDatabase()
            db.catalogEntityQueries.insertDeckIfAbsent("svc", "Service set", "", card_count = 10)
            db.catalogEntityQueries.updateDeckMeta("Service set", "", card_count = 10, id = "svc")

            val row =
                db.catalogEntityQueries
                    .selectAllDecks()
                    .awaitAsList()
                    .single()

            assertEquals(10L, row.card_count, "advertised count, not a join COUNT of 0")
            assertNull(row.adopted_at_epoch_ms, "still a passive Service-cache row")
        }

    @Test
    fun `only adopted decks are returned as owned and un-adopting clears them`() =
        runTest {
            val db = freshInMemoryDatabase()
            db.catalogEntityQueries.insertDeckIfAbsent("owned", "Owned", "", card_count = 0)
            db.catalogEntityQueries.insertDeckIfAbsent("cached", "Cached", "", card_count = 0)
            db.catalogEntityQueries.markDeckAdopted(adopted_at_epoch_ms = 42L, id = "owned")

            assertEquals(
                listOf("owned"),
                db.catalogEntityQueries
                    .selectOwnedDecks()
                    .awaitAsList()
                    .map { it.id },
                "a passive Service-cache deck is not owned",
            )

            db.catalogEntityQueries.clearDeckAdopted("owned")

            assertEquals(
                emptyList(),
                db.catalogEntityQueries
                    .selectOwnedDecks()
                    .awaitAsList()
                    .map { it.id },
            )
            assertNull(
                db.catalogEntityQueries
                    .selectAllDecks()
                    .awaitAsList()
                    .first { it.id == "owned" }
                    .adopted_at_epoch_ms,
                "un-adopt drops the deck back to a passive cache row",
            )
        }
}
