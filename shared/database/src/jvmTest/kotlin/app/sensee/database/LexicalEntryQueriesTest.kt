package app.sensee.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins the explicit-cleanup path that keeps the catalog from accumulating
 * abandoned drafts: a deleted entry is gone from both single and list reads,
 * and deleting an absent id is a harmless no-op (idempotent).
 */
class LexicalEntryQueriesTest {
    @Test
    fun `deleting an entry removes it from single and list reads`() =
        runTest {
            val db = freshInMemoryDatabase()
            db.lexicalEntryEntityQueries.upsertEntry("e1", "run", "Draft", "[]", 1L)
            db.lexicalEntryEntityQueries.upsertEntry("e2", "walk", "Confirmed", "[]", 2L)

            db.lexicalEntryEntityQueries.deleteEntry("e1")

            assertNull(db.lexicalEntryEntityQueries.selectEntry("e1").awaitAsOneOrNull())
            assertEquals(
                listOf("e2"),
                db.lexicalEntryEntityQueries
                    .selectAllEntries()
                    .awaitAsList()
                    .map { it.id },
            )
        }

    @Test
    fun `deleting an absent id is a no-op`() =
        runTest {
            val db = freshInMemoryDatabase()
            db.lexicalEntryEntityQueries.upsertEntry("e1", "run", "Draft", "[]", 1L)

            db.lexicalEntryEntityQueries.deleteEntry("missing")

            assertEquals(
                1,
                db.lexicalEntryEntityQueries
                    .selectAllEntries()
                    .awaitAsList()
                    .size,
            )
        }
}
