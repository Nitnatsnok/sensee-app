package app.sensee.feature.vocabularyEditor.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.observability.analytics.NoOpAnalyticsTracker
import app.sensee.core.observability.crash.NoOpCrashReporter
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.logging.AppLogger
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.vocabularyEditor.domain.EntryStatus
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.lexicon.domain.EntryId
import app.sensee.lexicon.domain.Sense
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
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

    // Captures every `warn` call's lazily-built message so the tests can pin
    // that defensive fallbacks (unknown status / malformed JSON) actually log
    // instead of vanishing silently. Non-warn overrides are intentionally
    // empty — this fake only records the warn channel under test.
    @Suppress("EmptyFunctionBlock")
    private class RecordingDiagnostics : AppDiagnostics {
        val warnings: MutableList<String> = mutableListOf()
        override val logger: AppLogger =
            object : AppLogger {
                override fun tag(tag: String): AppLogger = this

                override fun verbose(
                    throwable: Throwable?,
                    message: () -> String,
                ) {}

                override fun debug(
                    throwable: Throwable?,
                    message: () -> String,
                ) {}

                override fun info(
                    throwable: Throwable?,
                    message: () -> String,
                ) {}

                override fun warn(
                    throwable: Throwable?,
                    message: () -> String,
                ) {
                    warnings += message()
                }

                override fun error(
                    throwable: Throwable?,
                    message: () -> String,
                ) {}
            }
        override val crashReporter = NoOpCrashReporter
        override val analyticsTracker = NoOpAnalyticsTracker
    }

    @Test
    fun `confirmed entries are not demoted by draft update or duplicate confirm`() =
        runTest {
            val repository = repository()
            val draft = repository.createDraft("come across")
            val confirmed = repository.confirmSenses(draft.id, listOf(Sense(translation = "наткнуться")))

            assertNull(repository.updateDraftTerm(draft.id, "come acros"))
            val duplicate = repository.confirmSenses(draft.id, listOf(Sense(translation = "попасться")))

            assertEquals(EntryStatus.Confirmed, duplicate.status)
            assertEquals("come across", duplicate.term)
            assertEquals(confirmed.senses, duplicate.senses)
            assertEquals(duplicate, repository.getEntry(draft.id))
        }

    @Test
    fun `a new capture of the same term merges into the existing entry instead of forking a duplicate`() =
        runTest {
            val repository = repository()
            val first = repository.createDraft("come across")
            val confirmed =
                repository.confirmSenses(
                    first.id,
                    listOf(
                        Sense(
                            translation = "наткнуться",
                            surfaceForm = SurfaceForm.parse("come across"),
                            unitType = GrammarUnitType.PhrasalVerb,
                        ),
                    ),
                )

            // The user re-captures the same term: a fresh draft, then confirm.
            val secondDraft = repository.createDraft("Come Across")
            assertNotEquals(first.id, secondDraft.id)
            val merged =
                repository.confirmSenses(
                    secondDraft.id,
                    listOf(
                        Sense(
                            translation = "производить впечатление",
                            surfaceForm = SurfaceForm.parse("come across [as]"),
                            unitType = GrammarUnitType.PhrasalVerb,
                        ),
                    ),
                )

            // The merge target is the original entry; the second draft was absorbed.
            assertEquals(confirmed.id, merged.id)
            assertEquals(2, merged.senses.size)
            assertEquals(listOf("наткнуться", "производить впечатление"), merged.senses.map { it.translation })
            assertNull(repository.getEntry(secondDraft.id))
            assertEquals(merged, repository.getEntry(confirmed.id))
        }

    @Test
    fun `merging a re-capture skips senses that already exist by content`() =
        runTest {
            val repository = repository()
            val sense =
                Sense(
                    translation = "наткнуться",
                    surfaceForm = SurfaceForm.parse("come across"),
                    unitType = GrammarUnitType.PhrasalVerb,
                )
            val first = repository.createDraft("come across")
            val confirmed = repository.confirmSenses(first.id, listOf(sense))

            val secondDraft = repository.createDraft("come across")
            val merged =
                repository.confirmSenses(
                    secondDraft.id,
                    listOf(
                        // Re-proposed sense: identical content key → must NOT duplicate.
                        sense.copy(explanation = "AI wrote it slightly differently this time"),
                        // A genuinely new sense joins the existing entry.
                        Sense(
                            translation = "производить впечатление",
                            surfaceForm = SurfaceForm.parse("come across [as]"),
                            unitType = GrammarUnitType.PhrasalVerb,
                        ),
                    ),
                )

            assertEquals(confirmed.id, merged.id)
            assertEquals(2, merged.senses.size)
            // First sense is the one already on file (unchanged explanation).
            assertEquals(sense, merged.senses.first())
        }

    @Test
    fun `merging a re-capture treats verifier lemma metadata as non-identifying`() =
        runTest {
            val repository = repository()
            val sense =
                Sense(
                    translation = "наткнуться",
                    surfaceForm = SurfaceForm.parse("come across"),
                    unitType = GrammarUnitType.PhrasalVerb,
                )
            val first = repository.createDraft("come across")
            val confirmed = repository.confirmSenses(first.id, listOf(sense))

            val secondDraft = repository.createDraft("come across")
            val merged =
                repository.confirmSenses(
                    secondDraft.id,
                    listOf(
                        sense.copy(
                            baseLemma = "come across",
                            headLemma = "come",
                        ),
                    ),
                )

            assertEquals(confirmed.id, merged.id)
            assertEquals(listOf(sense), merged.senses)
        }

    @Test
    fun `editing a confirmed entry replaces its saved term and senses`() =
        runTest {
            val repository = repository()
            val draft = repository.createDraft("come across")
            val confirmed = repository.confirmSenses(draft.id, listOf(Sense(translation = "наткнуться")))
            val editedSense =
                Sense(
                    translation = "производить впечатление",
                    surfaceForm = SurfaceForm.parse("come across [as]"),
                    unitType = GrammarUnitType.PhrasalVerb,
                )

            val updated = repository.updateConfirmedEntry(confirmed.id, "come across as", listOf(editedSense))

            assertEquals(confirmed.id, updated?.id)
            assertEquals("come across as", updated?.term)
            assertEquals(listOf(editedSense), updated?.senses)
            assertEquals(updated, repository.getEntry(confirmed.id))
        }

    @Test
    fun `editing a confirmed entry into an existing term merges instead of forking a duplicate`() =
        runTest {
            val repository = repository()
            val firstDraft = repository.createDraft("come across")
            val first =
                repository.confirmSenses(
                    firstDraft.id,
                    listOf(Sense(translation = "наткнуться")),
                )
            val secondDraft = repository.createDraft("come up")
            val second =
                repository.confirmSenses(
                    secondDraft.id,
                    listOf(Sense(translation = "возникать")),
                )
            val editedSense = Sense(translation = "подойти")

            val merged = repository.updateConfirmedEntry(second.id, "Come Across", listOf(editedSense))

            assertEquals(first.id, merged?.id)
            assertEquals(listOf("наткнуться", "подойти"), merged?.senses?.map { it.translation })
            assertNull(repository.getEntry(second.id))
            assertEquals(merged, repository.getEntry(first.id))
        }

    @Test
    fun `first confirm of two senses with the same content key persists one merged sense`() =
        runTest {
            // Same identity-bearing fields → same deriveSenseContentKey. Without
            // a dedup in the first-confirm path (B1) the row would persist BOTH
            // senses, forking SRS state and only collapsing on a later re-capture.
            val repository = repository()
            val draft = repository.createDraft("come across")
            val baseSense =
                Sense(
                    translation = "наткнуться",
                    surfaceForm = SurfaceForm.parse("come across"),
                    unitType = GrammarUnitType.PhrasalVerb,
                )
            val confirmed =
                repository.confirmSenses(
                    draft.id,
                    listOf(
                        baseSense,
                        // Same content key — only explanation differs (excluded
                        // from deriveSenseContentKey). Must NOT duplicate.
                        baseSense.copy(explanation = "AI worded it twice on first pass"),
                    ),
                )

            assertEquals(EntryStatus.Confirmed, confirmed.status)
            assertEquals(1, confirmed.senses.size)
            assertEquals(baseSense, confirmed.senses.single())
            assertEquals(confirmed, repository.getEntry(draft.id))
        }

    @Test
    fun `a row with an unknown status logs a warning and degrades to Draft on read`() =
        runTest {
            // Write a row with a bogus status directly via SQLDelight, then read
            // through the repository. The fallback is intentional (no migrations)
            // but the diagnostic surface must register the event — otherwise a
            // lost Confirmed row looks like the user lost their work silently.
            val driver =
                JdbcSqliteDriver(
                    JdbcSqliteDriver.IN_MEMORY,
                    Properties(),
                    SenseeDatabase.Schema.synchronous(),
                )
            val db = SenseeDatabase(driver)
            db.lexicalEntryEntityQueries.upsertEntry(
                id = "row-bogus",
                term = "come across",
                status = "AwaitingTriage", // not a member of EntryStatus
                senses_json = "[]",
                updated_at_epoch_ms = 0L,
            )
            val recording = RecordingDiagnostics()
            val repository =
                DurableVocabularyRepository(
                    databaseProvider = FakeDbProvider(db),
                    dispatchers = immediateAppDispatchers(),
                    json = Json,
                    clock = FixedClock,
                    appDiagnostics = recording,
                )

            val loaded = repository.getEntry(EntryId("row-bogus"))

            assertEquals(EntryStatus.Draft, loaded?.status)
            assertEquals(emptyList(), loaded?.senses)
            assertEquals(
                1,
                recording.warnings.count { it.contains("Unknown EntryStatus 'AwaitingTriage'") },
                "unknown status must surface through diagnostics, not vanish silently",
            )
        }

    @Test
    fun `a row with malformed senses_json logs a warning and yields empty senses on read`() =
        runTest {
            val driver =
                JdbcSqliteDriver(
                    JdbcSqliteDriver.IN_MEMORY,
                    Properties(),
                    SenseeDatabase.Schema.synchronous(),
                )
            val db = SenseeDatabase(driver)
            db.lexicalEntryEntityQueries.upsertEntry(
                id = "row-broken",
                term = "come across",
                status = "Confirmed",
                senses_json = "{not-an-array",
                updated_at_epoch_ms = 0L,
            )
            val recording = RecordingDiagnostics()
            val repository =
                DurableVocabularyRepository(
                    databaseProvider = FakeDbProvider(db),
                    dispatchers = immediateAppDispatchers(),
                    json = Json,
                    clock = FixedClock,
                    appDiagnostics = recording,
                )

            val loaded = repository.getEntry(EntryId("row-broken"))

            assertEquals(EntryStatus.Confirmed, loaded?.status)
            assertEquals(emptyList(), loaded?.senses)
            assertEquals(
                1,
                recording.warnings.count { it.contains("Malformed senses_json") },
                "malformed JSON must surface through diagnostics so the loss is debuggable",
            )
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
            appDiagnostics = noOpAppDiagnostics(),
        )
    }
}
