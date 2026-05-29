package app.sensee.feature.vocabularyEditor.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.vocabularyEditor.domain.EntryStatus
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.SurfaceForm
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
