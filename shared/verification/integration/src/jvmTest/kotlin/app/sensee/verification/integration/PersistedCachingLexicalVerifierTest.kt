package app.sensee.verification.integration

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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
    fun `a query with examplesToValidate bypasses both layers because findings are sentence-specific`() =
        runTest {
            // ADR-009 invariant: example findings depend on the concrete
            // sentences, so a lemma-keyed cache would return stale findings
            // for new sentences. Two identical-by-lemma queries that each
            // carry examplesToValidate must each hit the delegate.
            val sharedDb = freshDatabase()
            val sharedProvider = StaticDatabaseProvider(sharedDb)
            val counter = CountingLookup()
            val verifier = persisted(counter, sharedProvider)

            val baseQuery = query("come")
            val withExample =
                baseQuery.copy(
                    examplesToValidate =
                        listOf(
                            app.sensee.verification.core.ExampleHint(
                                sentence =
                                    app.sensee.verification.core.SentenceHint(
                                        listOf(
                                            app.sensee.verification.core.SentenceHint.Segment.Text(
                                                "She comes across.",
                                            ),
                                        ),
                                    ),
                            ),
                        ),
                )

            verifier.verify(withExample)
            verifier.verify(withExample)

            assertEquals(2, counter.calls, "example-validating queries must skip the cache entirely")
        }

    @Test
    fun `a source ttl of zero disables cache writes entirely`() =
        runTest {
            // computeTtlMillis returns 0 when any source advertises a zero TTL;
            // verify() then short-circuits writeToMemory/writeToDisk. Without
            // the bypass, an entry with TTL 0 would land in L1 but never
            // expire correctly under the LRU model (expires_at_epoch_ms == now).
            val sharedDb = freshDatabase()
            val sharedProvider = StaticDatabaseProvider(sharedDb)
            val counter = CountingLookup()
            val verifier = persisted(counter, sharedProvider, sourceTtlOverride = 0L)

            verifier.verify(query("come"))
            verifier.verify(query("come"))

            // No cache write → every call goes to the delegate.
            assertEquals(2, counter.calls, "ttl==0 must skip both L1 and L2 writes")
        }

    @Test
    fun `policy maxCacheAgeMillis caps the source TTL when shorter`() =
        runTest {
            // computeTtlMillis = min(per-source TTL, policy.maxCacheAgeMillis,
            // DEFAULT). When the policy TTL is shorter than the source's, the
            // policy wins. We exercise it by setting a long source TTL and a
            // very short policy TTL, then advancing the clock past the policy
            // TTL but before the source one — the L2 read must treat the row
            // as expired.
            val sharedDb = freshDatabase()
            val sharedProvider = StaticDatabaseProvider(sharedDb)
            val counter = CountingLookup()
            val clock = AdvanceableClock(0L)
            val firstSession =
                persisted(counter, sharedProvider, clock = clock, sourceTtlOverride = 10_000L)
            firstSession.verify(query("come").copy(policy = VerificationPolicy(maxCacheAgeMillis = 1_000L)))

            clock.advance(2_000L)
            val secondSession =
                persisted(counter, sharedProvider, clock = clock, sourceTtlOverride = 10_000L)
            secondSession.verify(query("come").copy(policy = VerificationPolicy(maxCacheAgeMillis = 1_000L)))

            assertEquals(
                2,
                counter.calls,
                "the shorter policy TTL (1s) must win over the source TTL (10s)",
            )
        }

    @Test
    fun `concurrent writes past the disk cap do not over-evict beyond the configured limit`() =
        runTest {
            // Pre-fills cache to one row below the cap, then fires two concurrent
            // verifies that both cross the [pruneInterval] threshold. Before the
            // writeMutex fix, both writers computed `excess = count - cap`
            // against the same pre-delete row count and each deleted that many
            // oldest rows — leaving the cache below cap by 2 * excess. With
            // serialized prune the second writer sees the post-delete count and
            // only trims its own overshoot.
            val sharedDb = freshDatabase()
            val sharedProvider = StaticDatabaseProvider(sharedDb)
            val counter = CountingLookup()
            val verifier =
                persisted(counter, sharedProvider).apply {
                    maxDiskEntries = 8
                    pruneInterval = 2
                }

            // Fill to capacity with serial verifies (one each — only the last
            // few should cross the prune interval).
            repeat(8) { index -> verifier.verify(query("term-$index")) }
            val countBefore = sharedDb.cacheRowCount()
            assertEquals(8, countBefore.toInt())

            // Two new keys force two prunes. Run them in parallel via async so
            // both writers can race the prune sequence. The fix serializes them
            // under writeMutex; without the fix the second writer's prune would
            // see the same pre-delete count and double-evict.
            coroutineScope {
                val a = async { verifier.verify(query("hot-a")) }
                val b = async { verifier.verify(query("hot-b")) }
                a.await()
                b.await()
            }

            val countAfter = sharedDb.cacheRowCount().toInt()
            // Eviction may overshoot the cap by at most one in-flight write
            // because the count is sampled before the just-written row's row
            // count fully settles, but it must NOT drop below `cap - 1`.
            assertTrue(
                countAfter in (8 - 1)..8,
                "expected cache around the cap (7..8), got $countAfter",
            )
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

    private suspend fun SenseeDatabase.cacheRowCount(): Long {
        // Tiny helper so the prune-cap test reads the row count without going
        // through the SqlDriver cursor protocol (which forces a List).
        var count = 0L
        val rows = lexicalVerificationCacheEntityQueries.countEntries().awaitAsOneOrNull()
        if (rows != null) count = rows
        return count
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
