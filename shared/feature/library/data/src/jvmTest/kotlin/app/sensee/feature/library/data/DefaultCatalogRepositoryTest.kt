package app.sensee.feature.library.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.network.NetworkConfig
import app.sensee.core.network.NetworkHttpClientFactory
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.database.DefaultDatabaseTransactionRunner
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.library.data.local.CatalogLocalDataSource
import app.sensee.feature.library.data.remote.CatalogMockFixtures
import app.sensee.feature.library.data.remote.CatalogRemoteDataSource
import app.sensee.feature.library.data.remote.DeckDto
import app.sensee.feature.library.data.remote.DeckListDto
import app.sensee.feature.library.domain.DeckId
import app.sensee.lexicon.data.DefaultSenseIdFactory
import app.sensee.lexicon.data.DefaultSenseRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultCatalogRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val fixtures = CatalogMockFixtures().fixtures

    private object FixedClock : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000L)
    }

    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    private class Fixture {
        private val provider =
            FakeDbProvider(
                SenseeDatabase(
                    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties(), SenseeDatabase.Schema.synchronous()),
                ),
            )
        private val transactionRunner = DefaultDatabaseTransactionRunner(provider)
        private val senseRepository =
            DefaultSenseRepository(
                databaseProvider = provider,
                transactionRunner = transactionRunner,
                dispatchers = immediateAppDispatchers(),
                json = Json,
                clock = FixedClock,
                senseIdFactory = DefaultSenseIdFactory(),
                appDiagnostics = noOpAppDiagnostics(),
            )
        val localDataSource =
            CatalogLocalDataSource(
                databaseProvider = provider,
                transactionRunner = transactionRunner,
                senseReadRepository = senseRepository,
                senseWriteRepository = senseRepository,
                dispatchers = immediateAppDispatchers(),
                appDiagnostics = noOpAppDiagnostics(),
            )
    }

    @Test
    fun `load deck reads the local projection without fetching remote content`() =
        runBlocking {
            val fixture = Fixture()
            val deckId = "phrasal-verbs-come"
            val deck = json.decodeFromString<DeckDto>(fixtures.getValue("practice/decks/$deckId"))
            fixture.localDataSource.ingestDeck(deck, subscribe = true)
            val repository =
                DefaultCatalogRepository(
                    localDataSource = fixture.localDataSource,
                    remoteDataSource =
                        remoteDataSource(
                            contentForPath = { path ->
                                error("loadDeck must not fetch remote path $path")
                            },
                        ),
                    appDiagnostics = noOpAppDiagnostics(),
                )

            val loaded = repository.loadDeck(DeckId(deckId))

            assertEquals(deckId, loaded.deck.id.value)
            assertEquals(deck.cards.size, loaded.cards.size)
        }

    @Test
    fun `refresh from remote syncs already subscribed service deck content explicitly`() =
        runBlocking {
            val fixture = Fixture()
            val deckId = "phrasal-verbs-come"
            val deckSummary =
                json
                    .decodeFromString<DeckListDto>(fixtures.getValue("practice/decks"))
                    .decks
                    .first { it.id == deckId }
            fixture.localDataSource.upsertDeckSummaries(
                listOf(deckSummary),
            )
            fixture.localDataSource.setDeckSubscribed(deckId, subscribed = true)
            val repository =
                DefaultCatalogRepository(
                    localDataSource = fixture.localDataSource,
                    remoteDataSource = remoteDataSource(fixtures::getValue),
                    appDiagnostics = noOpAppDiagnostics(),
                )

            repository.refreshFromRemote()
            val loaded = repository.loadDeck(DeckId(deckId))

            assertEquals(deckId, loaded.deck.id.value)
            assertEquals(10, loaded.cards.size)
        }

    @Test
    fun `refresh keeps syncing other subscribed decks when one deck content fetch fails`() =
        runBlocking {
            val fixture = Fixture()
            val failing = "phrasal-verbs-come"
            val healthy = "architecture-basics"
            val summaries =
                json
                    .decodeFromString<DeckListDto>(fixtures.getValue("practice/decks"))
                    .decks
                    .filter { it.id == failing || it.id == healthy }
            fixture.localDataSource.upsertDeckSummaries(summaries)
            summaries.forEach { fixture.localDataSource.setDeckSubscribed(it.id, subscribed = true) }
            val repository =
                DefaultCatalogRepository(
                    localDataSource = fixture.localDataSource,
                    remoteDataSource =
                        remoteDataSource(
                            contentForPath = fixtures::getValue,
                            failPath = "practice/decks/$failing",
                        ),
                    appDiagnostics = noOpAppDiagnostics(),
                )

            repository.refreshFromRemote()

            assertEquals(
                10,
                repository.loadDeck(DeckId(healthy)).cards.size,
                "a healthy deck still syncs its content",
            )
            assertEquals(
                0,
                repository.loadDeck(DeckId(failing)).cards.size,
                "the failed deck is skipped, not fatal — its content stays unsynced until the next refresh",
            )
        }

    @Test
    fun `preview reads a subscribed deck from the local cache without fetching remote`() =
        runBlocking {
            val fixture = Fixture()
            val deckId = "phrasal-verbs-come"
            val deck = json.decodeFromString<DeckDto>(fixtures.getValue("practice/decks/$deckId"))
            fixture.localDataSource.ingestDeck(deck, subscribe = true)
            val repository =
                DefaultCatalogRepository(
                    localDataSource = fixture.localDataSource,
                    remoteDataSource =
                        remoteDataSource(
                            contentForPath = { path -> error("previewDeck must not fetch remote path $path") },
                        ),
                    appDiagnostics = noOpAppDiagnostics(),
                )

            val preview = repository.previewDeck(DeckId(deckId))

            assertEquals(deckId, preview.deck.id.value)
            assertEquals(deck.cards.size, preview.cards.size)
        }

    @Test
    fun `preview projects an unsubscribed service deck from remote without ingesting it`() =
        runBlocking {
            val fixture = Fixture()
            val deckId = "phrasal-verbs-come"
            val summary =
                json
                    .decodeFromString<DeckListDto>(fixtures.getValue("practice/decks"))
                    .decks
                    .first { it.id == deckId }
            fixture.localDataSource.upsertDeckSummaries(listOf(summary))
            val repository =
                DefaultCatalogRepository(
                    localDataSource = fixture.localDataSource,
                    remoteDataSource = remoteDataSource(fixtures::getValue),
                    appDiagnostics = noOpAppDiagnostics(),
                )

            val preview = repository.previewDeck(DeckId(deckId))

            assertEquals(10, preview.cards.size, "an unadopted service deck is browsable from remote")
            assertEquals(
                0,
                repository.loadDeck(DeckId(deckId)).cards.size,
                "previewing did not ingest the deck — its local content stays empty until adoption",
            )
        }

    private fun remoteDataSource(
        contentForPath: (String) -> String,
        failPath: String? = null,
    ): CatalogRemoteDataSource =
        CatalogRemoteDataSource(
            NetworkHttpClientFactory.create(
                engine =
                    MockEngine { request ->
                        val path = request.url.encodedPath.removePrefix("/")
                        if (path == failPath) error("remote down for $path")
                        respond(
                            content = contentForPath(path),
                            status = HttpStatusCode.OK,
                            headers =
                                headersOf(
                                    HttpHeaders.ContentType,
                                    ContentType.Application.Json.toString(),
                                ),
                        )
                    },
                config = NetworkConfig(baseUrl = "https://fixture.test"),
                json = json,
            ),
        )
}
