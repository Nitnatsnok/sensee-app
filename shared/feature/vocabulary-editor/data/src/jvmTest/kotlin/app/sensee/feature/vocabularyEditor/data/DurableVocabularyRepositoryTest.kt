package app.sensee.feature.vocabularyEditor.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.vocabularyEditor.domain.EntryStatus
import app.sensee.feature.vocabularyEditor.domain.Meaning
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class DurableVocabularyRepositoryTest {
    private object FixedClock : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000L)
    }

    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    @Test
    fun `confirmed entries are not demoted by draft update or duplicate confirm`() =
        runTest {
            val repository = repository()
            val draft = repository.createDraft("come across")
            val confirmed = repository.confirmMeanings(draft.id, listOf(Meaning(translation = "наткнуться")))

            assertNull(repository.updateDraftTerm(draft.id, "come acros"))
            val duplicate = repository.confirmMeanings(draft.id, listOf(Meaning(translation = "попасться")))

            assertEquals(EntryStatus.Confirmed, duplicate.status)
            assertEquals("come across", duplicate.term)
            assertEquals(confirmed.meanings, duplicate.meanings)
            assertEquals(duplicate, repository.getEntry(draft.id))
        }

    private fun repository(): DurableVocabularyRepository {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY,
                Properties(),
                SenseeDatabase.Schema.synchronous(),
            )
        return DurableVocabularyRepository(
            databaseProvider = FakeDbProvider(SenseeDatabase(driver)),
            dispatchers = immediateAppDispatchers(),
            json = Json,
            clock = FixedClock,
        )
    }
}
