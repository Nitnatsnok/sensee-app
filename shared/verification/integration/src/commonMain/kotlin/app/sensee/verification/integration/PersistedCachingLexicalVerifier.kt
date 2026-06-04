package app.sensee.verification.integration

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.database.LexicalVerificationCacheEntityQueries
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.logging.AppLogger
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.verification.core.contract.LexicalSource
import app.sensee.verification.core.contract.LexicalVerificationQuery
import app.sensee.verification.core.contract.LexicalVerificationReport
import app.sensee.verification.core.contract.LexicalVerifier
import app.sensee.verification.core.contract.LicensePolicy
import app.sensee.verification.core.contract.VerifierAvailability
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Two-layer cache for [LexicalVerifier] reports. The L1 layer is an in-memory
 * LRU sized for the active capture session; the L2 layer is SQLDelight-backed
 * so a cold launch still benefits from prior lookups.
 *
 * License-gated: a report is persisted to L2 only when every source in
 * [LexicalVerificationReport.sources] sets [LicensePolicy.storeContentAllowed]
 * to `true`. A report with even one strict-store source rides only the L1 —
 * the persistent layer respects the commercial-dictionary fence the
 * [LexicalSource] catalog encodes.
 *
 * TTL is the minimum of every source's [LicensePolicy.maxCacheTtlMillis], the
 * policy-level [VerificationPolicy.maxCacheAgeMillis], and a default. The
 * same TTL gates both layers; an expired L2 entry is deleted lazily on read.
 */
@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<LexicalVerifier>(),
)
@Inject
public class PersistedCachingLexicalVerifier(
    private val delegate: RoutingLexicalVerifier,
    private val databaseProvider: SenseeDatabaseProvider,
    private val json: Json,
    private val clock: Clock,
    appDiagnostics: AppDiagnostics,
) : LexicalVerifier {
    private val logger = appDiagnostics.logger.tag("PersistedVerifierCache")
    private val mutex = Mutex()

    // Serializes L2 writes (upsert + amortised prune) so concurrent writers
    // do not over-evict against a shared pre-delete row count.
    private val writeMutex = Mutex()
    private val memory = linkedMapOf<CacheKey, CacheEntry>()
    private var startupCleanupDone = false
    private var writesSinceLastPrune = 0

    // Test-only overrides for the L2 capacity policy. Production code uses the
    // companion constants; tests downscale them so the prune path is reachable
    // in milliseconds rather than thousands of writes.
    internal var maxDiskEntries: Int = MAX_DISK_ENTRIES
    internal var pruneInterval: Int = PRUNE_INTERVAL
    private val sourceCatalogById: Map<String, LexicalSource> = delegate.sourceCatalog.associateBy { it.id }
    private val sourceCatalogSignature: String = CacheKey.sourceCatalogSignature(delegate.sourceCatalog)

    override suspend fun verify(query: LexicalVerificationQuery): LexicalVerificationReport {
        val querySummary = VerificationLogSummaries.query(query)
        val key = CacheKey.from(query, sourceCatalogSignature)
        val now = clock.now().toEpochMilliseconds()
        runStartupCleanupOnce(now)
        readFromMemory(key, now)?.let { report ->
            val reportSummary = VerificationLogSummaries.report(report)
            logger.debug {
                "Verification cache hit: layer=L1; $querySummary; $reportSummary"
            }
            return report
        }
        readFromDisk(key, now)?.let { hit ->
            promoteToMemory(key, hit)
            val reportSummary = VerificationLogSummaries.report(hit.report)
            logger.debug {
                "Verification cache hit: layer=L2; expiresInMs=${hit.expiresAtEpochMillis - now}; " +
                    "$querySummary; $reportSummary"
            }
            return hit.report
        }
        logger.debug { "Verification cache miss: $querySummary" }
        val report = delegate.verify(query)
        val reportSummary = VerificationLogSummaries.report(report)
        val ttl = computeTtlMillis(report, query.policy.maxCacheAgeMillis)
        if (ttl > 0L) {
            val entry = CacheEntry(report = report, expiresAtEpochMillis = now + ttl)
            writeToMemory(key, entry)
            logger.debug { "Verification cache write: layer=L1; ttlMs=$ttl; $reportSummary" }
            if (report.isPersistableForDisk()) {
                writeToDisk(key, entry, now)
            } else {
                val skipReason = VerificationLogSummaries.l2SkipReason(report)
                logger.debug {
                    "Verification cache write skipped: layer=L2; reason=$skipReason; ttlMs=$ttl; $reportSummary"
                }
            }
        } else {
            logger.debug { "Verification cache write skipped: reason=ttl-zero; $reportSummary" }
        }
        return report
    }

    private suspend fun runStartupCleanupOnce(now: Long) {
        val shouldCleanup =
            mutex.withLock {
                if (startupCleanupDone) {
                    false
                } else {
                    startupCleanupDone = true
                    true
                }
            }
        if (!shouldCleanup) return
        val queries = queriesOrNull() ?: return
        runCatchingCancellable {
            queries.deleteExpired(now)
        }.onFailure { throwable ->
            logger.warn(throwable) { "Startup cache cleanup failed; expired entries linger until next prune" }
        }
    }

    private suspend fun readFromMemory(
        key: CacheKey,
        now: Long,
    ): LexicalVerificationReport? =
        mutex.withLock {
            val entry = memory[key]?.takeIf { it.expiresAtEpochMillis > now }
            if (entry != null) {
                // LRU touch.
                memory.remove(key)
                memory[key] = entry
            }
            entry?.report
        }

    private suspend fun readFromDisk(
        key: CacheKey,
        now: Long,
    ): CacheEntry? {
        val queries = queriesOrNull() ?: return null
        return runCatchingCancellable {
            val persistedKey = key.persistedKey()
            val row = queries.selectVerificationByKey(persistedKey).awaitAsOneOrNull() ?: return null
            if (row.key_fingerprint != key.fingerprint()) {
                logger.warn { "Persistent cache fingerprint mismatch for '$persistedKey'; dropping row" }
                queries.deleteByKey(persistedKey)
                return null
            }
            if (row.expires_at_epoch_ms <= now) {
                queries.deleteByKey(persistedKey)
                return null
            }
            val report =
                try {
                    json.decodeFromString(LexicalVerificationReport.serializer(), row.report_json)
                } catch (decode: SerializationException) {
                    // Drop the malformed row; the next live lookup repopulates it.
                    logger.warn(decode) { "Persistent cache row for '$persistedKey' could not be decoded; dropping" }
                    queries.deleteByKey(persistedKey)
                    return null
                }
            val rehydratedReport =
                rehydrateFromCurrentSourceCatalog(
                    report = report,
                    sourceCatalogById = sourceCatalogById,
                    persistedKey = persistedKey,
                    queries = queries,
                    logger = logger,
                )
                    ?: return null
            if (now - row.last_accessed_at_epoch_ms >= TOUCH_THROTTLE_MILLIS) {
                queries.touchAccess(now, persistedKey)
            }
            CacheEntry(report = rehydratedReport, expiresAtEpochMillis = row.expires_at_epoch_ms)
        }.getOrElse { throwable ->
            logger.warn(throwable) { "Persistent cache read failed; falling through to live lookup" }
            null
        }
    }

    private suspend fun promoteToMemory(
        key: CacheKey,
        entry: CacheEntry,
    ) {
        mutex.withLock {
            memory[key] = entry
            evictIfNeeded()
        }
    }

    private suspend fun writeToMemory(
        key: CacheKey,
        entry: CacheEntry,
    ) {
        mutex.withLock {
            memory[key] = entry
            evictIfNeeded()
        }
    }

    private fun evictIfNeeded() {
        while (memory.size > MAX_MEMORY_ENTRIES) {
            val iterator = memory.entries.iterator()
            if (!iterator.hasNext()) break
            iterator.next()
            iterator.remove()
        }
    }

    private suspend fun writeToDisk(
        key: CacheKey,
        entry: CacheEntry,
        now: Long,
    ) {
        val queries = queriesOrNull() ?: return
        runCatchingCancellable {
            val payload = json.encodeToString(LexicalVerificationReport.serializer(), entry.report)
            // Serialize the upsert+prune pair under [writeMutex] so two
            // concurrent writers crossing the [PRUNE_INTERVAL] boundary cannot
            // each compute `excess = count - MAX_DISK_ENTRIES` against the same
            // pre-delete count and double-evict (cache thrash, ADR-0009). A
            // SQLDelight transaction would not help here — the over-eviction
            // race is between two separate writes' eviction decisions, not
            // about an atomic statement group.
            writeMutex.withLock {
                queries.upsert(
                    cache_key = key.persistedKey(),
                    key_fingerprint = key.fingerprint(),
                    report_json = payload,
                    written_at_epoch_ms = now,
                    expires_at_epoch_ms = entry.expiresAtEpochMillis,
                    last_accessed_at_epoch_ms = now,
                )
                logger.debug {
                    "Verification cache write: layer=L2; ttlMs=${entry.expiresAtEpochMillis - now}; " +
                        VerificationLogSummaries.report(entry.report)
                }
                pruneIfOverCapLocked(queries)
            }
        }.onFailure { throwable ->
            when (throwable) {
                is SerializationException ->
                    logger.warn(throwable) { "Persistent cache write skipped — report not serializable" }
                else -> logger.warn(throwable) { "Persistent cache write failed; in-memory copy still applies" }
            }
        }
    }

    // Caller MUST hold [writeMutex]: the count/select/delete sequence must run
    // exclusive of any other writer's prune decision, otherwise two writers
    // crossing PRUNE_INTERVAL each compute excess against the same pre-delete
    // count and delete `2 * excess` rows.
    private suspend fun pruneIfOverCapLocked(queries: LexicalVerificationCacheEntityQueries) {
        writesSinceLastPrune++
        if (writesSinceLastPrune < pruneInterval) return
        writesSinceLastPrune = 0
        val count = queries.countEntries().awaitAsOneOrNull() ?: return
        if (count <= maxDiskEntries) return
        val excess = (count - maxDiskEntries).toInt()
        val keysToDrop = queries.selectOldestKeys(excess.toLong()).awaitAsList()
        keysToDrop.forEach { key -> queries.deleteByKey(key) }
    }

    private suspend fun queriesOrNull(): LexicalVerificationCacheEntityQueries? =
        runCatchingCancellable {
            databaseProvider.database().lexicalVerificationCacheEntityQueries
        }.getOrElse { throwable ->
            logger.warn(throwable) { "Database unavailable; cache falls back to in-memory only" }
            null
        }

    private fun computeTtlMillis(
        report: LexicalVerificationReport,
        policyMaxCacheAgeMillis: Long?,
    ): Long {
        val sourceMin =
            report.sources
                .mapNotNull { it.license.maxCacheTtlMillis }
                .minOrNull()
        val candidates = listOfNotNull(sourceMin, policyMaxCacheAgeMillis, DEFAULT_TTL_MILLIS)
        return candidates.min()
    }

    private data class CacheKey(
        val term: String,
        val studyLanguageTag: String,
        val expected: ExpectedQueryKey,
        val policy: PolicyKey,
        val sourceCatalogSignature: String,
    ) {
        fun persistedKey(): String = "v5:${digest64Hex("key:${serializedParts()}")}"

        fun fingerprint(): String = digest64Hex("fingerprint:${serializedParts()}")

        private fun serializedParts(): String =
            buildString {
                appendPart("term", term)
                appendPart("studyLanguageTag", studyLanguageTag)
                appendPart("nativeLanguageTag", expected.nativeLanguageTag.orEmpty())
                appendPart("expectedEntryType", expected.entryType.orEmpty())
                appendPart("expectedPartOfSpeech", expected.partOfSpeech.orEmpty())
                appendPart("timeoutPerProviderMillis", policy.timeoutPerProviderMillis.toString())
                appendPart("maxCacheAgeMillis", policy.maxCacheAgeMillis?.toString().orEmpty())
                appendPart("includeFamily", policy.includeFamily.toString())
                appendPart("familySiblingCap", policy.familySiblingCap.toString())
                appendPart("allowNetwork", policy.allowNetwork.toString())
                appendPart("sourceCatalog", sourceCatalogSignature)
            }

        companion object {
            private const val SEP: Char = '\u001F'
            private const val SUB_SEP: Char = '\u001E'
            private const val FNV_OFFSET_BASIS: Long = -3750763034362895579L // 0xcbf29ce484222325
            private const val FNV_PRIME: Long = 1099511628211L
            private const val RADIX_HEX: Int = 16

            fun from(
                query: LexicalVerificationQuery,
                sourceCatalogSignature: String,
            ): CacheKey =
                CacheKey(
                    term = query.text.trim().lowercase(),
                    studyLanguageTag = query.studyLanguageTag.lowercase(),
                    expected =
                        ExpectedQueryKey(
                            nativeLanguageTag = query.nativeLanguageTag?.lowercase(),
                            entryType = query.expectedEntryType?.id?.lowercase(),
                            partOfSpeech = query.expectedPartOfSpeech?.id?.lowercase(),
                        ),
                    policy =
                        PolicyKey(
                            timeoutPerProviderMillis = query.policy.timeoutPerProviderMillis,
                            maxCacheAgeMillis = query.policy.maxCacheAgeMillis,
                            includeFamily = query.policy.includeFamily,
                            familySiblingCap = query.policy.familySiblingCap,
                            allowNetwork = query.policy.allowNetwork,
                        ),
                    sourceCatalogSignature = sourceCatalogSignature,
                )

            fun sourceCatalogSignature(sources: Set<LexicalSource>): String =
                digest64Hex(
                    sources
                        .sortedBy { it.id }
                        .joinToString(SUB_SEP.toString()) { source ->
                            val maxCacheTtlMillis =
                                source.license.maxCacheTtlMillis
                                    ?.toString()
                                    .orEmpty()
                            val fields =
                                buildList {
                                    add(source.id)
                                    add(source.versionTagOrNull().orEmpty())
                                    add(source.license.storeContentAllowed.toString())
                                    add(maxCacheTtlMillis)
                                    add(source.license.usableAsLlmContext.toString())
                                }
                            fields.joinToString(SOURCE_FIELD_SEP.toString()) { value ->
                                "${value.length}:$value"
                            }
                        },
                )

            private fun StringBuilder.appendPart(
                name: String,
                value: String,
            ) {
                if (isNotEmpty()) append(SEP)
                append(name)
                    .append('=')
                    .append(value.length)
                    .append(':')
                    .append(value)
            }

            private fun digest64Hex(value: String): String {
                var hash = FNV_OFFSET_BASIS
                for (byte in value.encodeToByteArray()) {
                    hash = (hash xor (byte.toLong() and BYTE_MASK)) * FNV_PRIME
                }
                return hash.toULong().toString(RADIX_HEX)
            }

            private const val BYTE_MASK: Long = 0xffL
        }
    }

    private data class ExpectedQueryKey(
        val nativeLanguageTag: String?,
        val entryType: String?,
        val partOfSpeech: String?,
    )

    private data class PolicyKey(
        val timeoutPerProviderMillis: Long,
        val maxCacheAgeMillis: Long?,
        val includeFamily: Boolean,
        val familySiblingCap: Int,
        val allowNetwork: Boolean,
    )

    private data class CacheEntry(
        val report: LexicalVerificationReport,
        val expiresAtEpochMillis: Long,
    )

    public companion object {
        public const val DEFAULT_TTL_MILLIS: Long = 24L * 60L * 60L * 1000L
        public const val MAX_MEMORY_ENTRIES: Int = 256
        public const val MAX_DISK_ENTRIES: Int = 4096

        // Minimum interval between disk touch-updates; coalesces hot-key writes
        // while keeping LRU resolution adequate.
        internal const val TOUCH_THROTTLE_MILLIS: Long = 60L * 1000L

        // Prune is amortised over this many writes.
        internal const val PRUNE_INTERVAL: Int = 64
    }
}

private suspend fun rehydrateFromCurrentSourceCatalog(
    report: LexicalVerificationReport,
    sourceCatalogById: Map<String, LexicalSource>,
    persistedKey: String,
    queries: LexicalVerificationCacheEntityQueries,
    logger: AppLogger,
): LexicalVerificationReport? {
    val currentSources = mutableListOf<LexicalSource>()
    for (persisted in report.sources) {
        val current =
            sourceCatalogById[persisted.id]
                ?: return dropStaleRow(
                    queries = queries,
                    persistedKey = persistedKey,
                    reason = "source-missing:${persisted.id}",
                    logger = logger,
                )
        if (current.versionTagOrNull() != persisted.versionTagOrNull()) {
            return dropStaleRow(
                queries = queries,
                persistedKey = persistedKey,
                reason = "source-version-changed:${persisted.id}",
                logger = logger,
            )
        }
        if (!current.license.storeContentAllowed) {
            return dropStaleRow(
                queries = queries,
                persistedKey = persistedKey,
                reason = "source-no-longer-persistable:${persisted.id}",
                logger = logger,
            )
        }
        currentSources += current
    }
    val rehydrated = report.copy(sources = currentSources)
    if (!rehydrated.isPersistableForDisk()) {
        return dropStaleRow(
            queries = queries,
            persistedKey = persistedKey,
            reason = "report-no-longer-persistable",
            logger = logger,
        )
    }
    return rehydrated
}

private suspend fun dropStaleRow(
    queries: LexicalVerificationCacheEntityQueries,
    persistedKey: String,
    reason: String,
    logger: AppLogger,
): LexicalVerificationReport? {
    logger.warn { "Persistent cache row for '$persistedKey' is stale; reason=$reason; dropping" }
    queries.deleteByKey(persistedKey)
    return null
}

private fun LexicalVerificationReport.isPersistableForDisk(): Boolean =
    availability is VerifierAvailability.Available &&
        sources.isNotEmpty() &&
        sources.all { it.license.storeContentAllowed }

private const val SOURCE_FIELD_SEP: Char = '\u001D'

private fun LexicalSource.versionTagOrNull(): String? =
    when (this) {
        is LexicalSource.Adapter -> versionTag
    }
