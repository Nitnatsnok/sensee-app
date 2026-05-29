package app.sensee.verification.integration

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.observability.analytics.NoOpAnalyticsTracker
import app.sensee.core.observability.crash.NoOpCrashReporter
import app.sensee.core.observability.diagnostics.DefaultAppDiagnostics
import app.sensee.core.observability.logging.DefaultAppLoggerFactory
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.verification.core.AttributionPolicy
import app.sensee.verification.core.Confidence
import app.sensee.verification.core.LexicalEntryLookup
import app.sensee.verification.core.LexicalEntryLookupResult
import app.sensee.verification.core.LexicalEntryTypeHint
import app.sensee.verification.core.LexicalExistence
import app.sensee.verification.core.LexicalSource
import app.sensee.verification.core.LexicalSourceRef
import app.sensee.verification.core.LexicalVerificationQuery
import app.sensee.verification.core.LicensePolicy
import app.sensee.verification.core.NormalizationOutcome
import app.sensee.verification.core.SenseHint
import app.sensee.verification.core.VerificationPolicy
import app.sensee.verification.core.VerifierAvailability
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Clock
import kotlin.time.Instant

class PersistedCachingLexicalVerifierTest {
    private val testJson =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }

    @Test
    fun `a cold restart hits the persisted layer without re-invoking the network leg`() =
        runTest {
            val sharedDb = freshDatabase()
            val sharedProvider = StaticDatabaseProvider(sharedDb)
            val counter = CountingLookup()
            val firstSession = persisted(counter, sharedProvider)
            firstSession.verify(query("come across"))
            assertEquals(1, counter.calls)

            // Simulate a process restart: build a brand-new verifier instance
            // but reuse the same backing database. In-memory L1 is empty;
            // only the persistent L2 can answer.
            val secondSession = persisted(counter, sharedProvider)
            secondSession.verify(query("come across"))

            assertEquals(1, counter.calls, "second session must hit the persisted layer, not the delegate")
        }

    @Test
    fun `an expired persisted entry is refetched and refreshed`() =
        runTest {
            val sharedDb = freshDatabase()
            val sharedProvider = StaticDatabaseProvider(sharedDb)
            val counter = CountingLookup()
            val clock = AdvanceableClock(0L)
            val firstSession = persisted(counter, sharedProvider, clock = clock, sourceTtlOverride = 1_000L)
            firstSession.verify(query("come across"))

            clock.advance(2_000L)
            val secondSession = persisted(counter, sharedProvider, clock = clock, sourceTtlOverride = 1_000L)
            secondSession.verify(query("come across"))

            assertEquals(2, counter.calls, "expired persistent entry must trigger a refetch")
        }

    @Test
    fun `a strict-store source skips persistence but keeps in-memory caching`() =
        runTest {
            val sharedDb = freshDatabase()
            val sharedProvider = StaticDatabaseProvider(sharedDb)
            val counter = CountingLookup()
            val firstSession = persisted(counter, sharedProvider, allowStorage = false)
            firstSession.verify(query("come across"))

            // Cold restart: in-memory is gone, persistent layer was never
            // written because the license said no. The delegate has to run again.
            val secondSession = persisted(counter, sharedProvider, allowStorage = false)
            secondSession.verify(query("come across"))

            assertEquals(2, counter.calls, "strict-store source must not have been persisted")
        }

    @Test
    fun `cache key separates behavior-affecting query hints`() =
        runTest {
            val sharedDb = freshDatabase()
            val sharedProvider = StaticDatabaseProvider(sharedDb)
            val counter = CountingLookup()
            val verifier = persisted(counter, sharedProvider)

            verifier.verify(query("come").copy(expectedEntryType = LexicalEntryTypeHint("word")))
            verifier.verify(query("come").copy(expectedEntryType = LexicalEntryTypeHint("phrasal_verb")))

            assertEquals(2, counter.calls, "entry-type-specific queries must not share one cache row")
        }

    @Test
    fun `persisted cache key does not store sense hint prose in plaintext`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            SenseeDatabase.Schema.create(driver).await()
            val sharedProvider = StaticDatabaseProvider(SenseeDatabase(driver))
            val counter = CountingLookup()
            val verifier = persisted(counter, sharedProvider)

            verifier.verify(
                query("come").copy(
                    senseHints =
                        listOf(
                            SenseHint(
                                id = "private-sense",
                                definition = "private definition from user notes",
                                translation = "private translation",
                                example = "private example sentence",
                            ),
                        ),
                ),
            )

            val row = driver.cacheRows().single()
            assertEquals(true, row.key.startsWith("v4:"))
            assertFalse(row.key.contains("private"), "persisted primary key must not expose raw hint text")
            assertFalse(row.fingerprint.contains("private"), "persisted fingerprint must not expose raw hint text")
            assertEquals(1, counter.calls)
        }

    @Test
    fun `cache key separates allow network policy`() =
        runTest {
            val sharedDb = freshDatabase()
            val sharedProvider = StaticDatabaseProvider(sharedDb)
            val counter = CountingLookup()
            val verifier = persisted(counter, sharedProvider)

            verifier.verify(query("come across").copy(policy = VerificationPolicy(allowNetwork = false)))
            verifier.verify(query("come across"))

            assertEquals(2, counter.calls, "offline and online policies must use distinct cache keys")
        }

    @Test
    fun `a report without source metadata is not persisted`() =
        runTest {
            val sharedDb = freshDatabase()
            val sharedProvider = StaticDatabaseProvider(sharedDb)
            val counter = CountingLookup(includeSourceRef = false)
            val firstSession = persisted(counter, sharedProvider)
            firstSession.verify(query("come across"))

            val secondSession = persisted(counter, sharedProvider)
            secondSession.verify(query("come across"))

            assertEquals(2, counter.calls, "source-less reports are only safe for in-memory caching")
        }

    @Test
    fun `a source version change invalidates persisted verification cache`() =
        runTest {
            val sharedDb = freshDatabase()
            val sharedProvider = StaticDatabaseProvider(sharedDb)
            val counter = CountingLookup()
            val firstSession = persisted(counter, sharedProvider, sourceVersionTag = "v1")
            firstSession.verify(query("come across"))

            val secondSession = persisted(counter, sharedProvider, sourceVersionTag = "v2")
            secondSession.verify(query("come across"))

            assertEquals(2, counter.calls, "stale source-version metadata must force a live refetch")
        }

    @Test
    fun `a storage policy change invalidates persisted verification cache`() =
        runTest {
            val sharedDb = freshDatabase()
            val sharedProvider = StaticDatabaseProvider(sharedDb)
            val counter = CountingLookup()
            val firstSession = persisted(counter, sharedProvider, allowStorage = true)
            firstSession.verify(query("come across"))

            val strictSession = persisted(counter, sharedProvider, allowStorage = false)
            strictSession.verify(query("come across"))
            val nextStrictSession = persisted(counter, sharedProvider, allowStorage = false)
            nextStrictSession.verify(query("come across"))

            assertEquals(3, counter.calls, "a strict current catalog must not reuse old persisted content")
        }

    private fun persisted(
        lookup: LexicalEntryLookup,
        provider: SenseeDatabaseProvider,
        clock: AdvanceableClock = AdvanceableClock(0L),
        sourceTtlOverride: Long? = null,
        allowStorage: Boolean = true,
        sourceVersionTag: String? = null,
    ): PersistedCachingLexicalVerifier {
        val source =
            LexicalSource.Adapter(
                id = "stub",
                displayName = "Test stub",
                attribution = AttributionPolicy(required = false),
                license =
                    LicensePolicy(
                        storeContentAllowed = allowStorage,
                        usableAsLlmContext = true,
                        maxCacheTtlMillis = sourceTtlOverride,
                    ),
                versionTag = sourceVersionTag,
            )
        val diagnostics =
            DefaultAppDiagnostics(
                logger = DefaultAppLoggerFactory().tagged("Test"),
                crashReporter = NoOpCrashReporter,
                analyticsTracker = NoOpAnalyticsTracker,
            )
        val routing =
            RoutingLexicalVerifier(
                contributors =
                    VerificationContributors(
                        entryLookups = setOf(lookup),
                        frequencyProviders = emptySet(),
                        cefrProviders = emptySet(),
                        senseInventoryProviders = emptySet(),
                        exampleQualityCheckers = emptySet(),
                        familyProviders = emptySet(),
                    ),
                sourceCatalog = setOf(source),
                appDiagnostics = diagnostics,
            )
        return PersistedCachingLexicalVerifier(
            delegate = routing,
            databaseProvider = provider,
            json = testJson,
            clock = clock,
            appDiagnostics = diagnostics,
        )
    }

    private fun query(text: String): LexicalVerificationQuery =
        LexicalVerificationQuery(text = text, studyLanguageTag = "en")

    private suspend fun freshDatabase(): SenseeDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SenseeDatabase.Schema.create(driver).await()
        return SenseeDatabase(driver)
    }

    private suspend fun SqlDriver.cacheRows(): List<CacheRow> =
        executeQuery(
            identifier = null,
            sql = "SELECT cache_key, key_fingerprint FROM lexical_verification_cache",
            mapper = { cursor ->
                val rows = mutableListOf<CacheRow>()
                while ((cursor.next() as QueryResult.Value).value) {
                    val key = cursor.getString(0)
                    val fingerprint = cursor.getString(1)
                    if (key != null && fingerprint != null) {
                        rows += CacheRow(key, fingerprint)
                    }
                }
                QueryResult.Value(rows)
            },
            parameters = 0,
        ).await()

    private data class CacheRow(
        val key: String,
        val fingerprint: String,
    )

    private class StaticDatabaseProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    private class CountingLookup(
        private val includeSourceRef: Boolean = true,
    ) : LexicalEntryLookup {
        var calls = 0
            private set

        override suspend fun lookup(query: LexicalVerificationQuery): LexicalEntryLookupResult {
            calls++
            return LexicalEntryLookupResult(
                availability = VerifierAvailability.Available,
                existence = LexicalExistence.Confirmed,
                normalized = NormalizationOutcome.EMPTY,
                entryType = null,
                partsOfSpeech = emptyList(),
                confidence = Confidence.Medium,
                sources =
                    if (includeSourceRef) {
                        listOf(LexicalSourceRef(sourceId = "stub", entryId = query.text, fetchedAtEpochMillis = 0L))
                    } else {
                        emptyList()
                    },
            )
        }
    }

    private class AdvanceableClock(
        start: Long,
    ) : Clock {
        private var nowMillis: Long = start

        fun advance(deltaMillis: Long) {
            nowMillis += deltaMillis
        }

        override fun now(): Instant = Instant.fromEpochMilliseconds(nowMillis)
    }
}
