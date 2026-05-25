package app.sensee.feature.library.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.library.data.local.CatalogLocalDataSource
import app.sensee.feature.library.data.remote.CatalogRemoteDataSource
import app.sensee.feature.vocabularyEditor.domain.EntryId
import app.sensee.feature.vocabularyEditor.domain.EntryStatus
import app.sensee.feature.vocabularyEditor.domain.LexicalEntry
import app.sensee.feature.vocabularyEditor.domain.Meaning
import app.sensee.feature.vocabularyEditor.domain.VocabularyRepository
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.fsrs.FsrsParameters
import app.sensee.srs.testKit.InMemorySrsStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class CapturedCatalogRepositoryTest {
    private object FixedClock : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000L)
    }

    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    private class FakeVocabularyRepository(
        private val entries: List<LexicalEntry>,
    ) : VocabularyRepository {
        override suspend fun createDraft(term: String): LexicalEntry = error("unused")

        override suspend fun updateDraftTerm(
            id: EntryId,
            term: String,
        ): LexicalEntry = error("unused")

        override suspend fun getEntry(id: EntryId): LexicalEntry? = entries.firstOrNull { it.id == id }

        override suspend fun listEntries(): List<LexicalEntry> = entries

        override fun observeEntries(): Flow<List<LexicalEntry>> = flowOf(entries)

        override suspend fun confirmMeanings(
            id: EntryId,
            meanings: List<Meaning>,
        ): LexicalEntry = error("unused")

        override suspend fun deleteEntry(id: EntryId) = Unit
    }

    private fun newRepository(srsStorage: InMemorySrsStorage<FsrsParameters>): CapturedCatalogRepository {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY,
                Properties(),
                SenseeDatabase.Schema.synchronous(),
            )
        val localDataSource =
            CatalogLocalDataSource(
                databaseProvider = FakeDbProvider(SenseeDatabase(driver)),
                srsStorage = srsStorage,
                dispatchers = immediateAppDispatchers(),
                json = Json,
                clock = FixedClock,
                appDiagnostics = noOpAppDiagnostics(),
            )
        val remoteDataSource =
            CatalogRemoteDataSource(
                HttpClient(
                    MockEngine {
                        respond(
                            content = """{"decks":[]}""",
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                ),
            )
        val base = DefaultCatalogRepository(localDataSource, remoteDataSource)
        return CapturedCatalogRepository(
            base = base,
            vocabulary =
                FakeVocabularyRepository(
                    listOf(
                        LexicalEntry(
                            id = EntryId("e1"),
                            term = "come across",
                            status = EntryStatus.Confirmed,
                            meanings = listOf(Meaning(translation = "наткнуться")),
                        ),
                    ),
                ),
            srsStorage = srsStorage,
        )
    }

    @Test
    fun `loading a captured deck materializes SRS rows before review`() =
        runTest {
            val srsStorage = InMemorySrsStorage(initialParameters = FsrsParameters.defaultV6())
            val repository = newRepository(srsStorage)

            val deck = repository.loadDeck(CapturedCatalogDerivation.DECK_ID)
            val card = deck.cards.single()

            assertNotNull(srsStorage.getCard(SrsCardId(card.id.value)))
            assertEquals(card.srs, srsStorage.getCard(SrsCardId(card.id.value)))
        }

    @Test
    fun `owned material stream surfaces the captured deck`() =
        runTest {
            val srsStorage = InMemorySrsStorage(initialParameters = FsrsParameters.defaultV6())
            val repository = newRepository(srsStorage)

            val owned = repository.observeOwnedMaterial().first()

            assertTrue(
                owned.any { it.id == CapturedCatalogDerivation.DECK_ID },
                "a confirmed capture entry appears as the captured deck without any adoption",
            )
        }
}
