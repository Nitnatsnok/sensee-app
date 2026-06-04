package app.sensee.feature.library.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.database.DefaultDatabaseTransactionRunner
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.library.domain.CardId
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.lexicon.data.DefaultSenseIdFactory
import app.sensee.lexicon.data.DefaultSenseRepository
import app.sensee.lexicon.domain.ContextualApplication
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.SenseId
import app.sensee.lexicon.domain.SenseOrigin
import app.sensee.lexicon.domain.SenseStatus
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.fsrs.FsrsParameters
import app.sensee.srs.testKit.InMemorySrsStorage
import app.sensee.srs.testKit.SrsTestCards
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * `claim` takes a detached Personal copy of a Service sense: a fresh sense_id, the
 * content copied, and SRS cloned onto the new id for the sense and its form cards —
 * all while the source sense and its SRS are left untouched.
 */
class ClaimTest {
    private object FixedClock : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000L)
    }

    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    private class Fixture {
        private val db =
            SenseeDatabase(
                JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties(), SenseeDatabase.Schema.synchronous()),
            )
        private val provider = FakeDbProvider(db)
        val srs = InMemorySrsStorage(initialParameters = FsrsParameters.defaultV6())
        val senseRepository =
            DefaultSenseRepository(
                provider,
                immediateAppDispatchers(),
                Json,
                FixedClock,
                DefaultSenseIdFactory(),
                noOpAppDiagnostics(),
            )
        val claim =
            DefaultClaimRepository(senseRepository, senseRepository, srs, DefaultDatabaseTransactionRunner(provider))
    }

    @Test
    fun `claim mints a detached personal copy and clones SRS for the sense and its forms`() =
        runTest {
            val fixture = Fixture()
            val service =
                fixture.senseRepository.upsert(
                    sense =
                        Sense(
                            translation = "бросать",
                            surfaceForm = SurfaceForm.parse("throw"),
                            contextualApplications =
                                listOf(
                                    ContextualApplication(StudiedSentence.parse("I [[throw]] it.")),
                                ),
                            irregularForms = IrregularForms("throw", "threw", "thrown"),
                        ),
                    status = SenseStatus.Confirmed,
                    origin = SenseOrigin.Service,
                    sourceRef = "card-x",
                )
            val formSuffix = ":form:${GrammarForm.PastTense.id}"
            fixture.srs.saveCard(SrsTestCards.newCard(service.id.value).copy(reviewCount = 7))
            fixture.srs.saveCard(SrsTestCards.newCard("${service.id.value}$formSuffix").copy(reviewCount = 3))

            val claimed = fixture.claim.claim(CardId(service.id.value))

            assertNotEquals(service.id.value, claimed.value, "claim mints a fresh Personal sense_id")
            val copy = fixture.senseRepository.getById(SenseId(claimed.value))
            assertEquals(SenseOrigin.Personal, copy?.origin, "the copy is Personal")
            assertEquals("бросать", copy?.sense?.translation, "content is copied")

            assertEquals(
                7,
                fixture.srs.getCard(SrsCardId(claimed.value))?.reviewCount,
                "base SRS cloned onto the new id",
            )
            assertEquals(
                3,
                fixture.srs.getCard(SrsCardId("${claimed.value}$formSuffix"))?.reviewCount,
                "form SRS cloned onto the new id",
            )

            assertEquals(7, fixture.srs.getCard(SrsCardId(service.id.value))?.reviewCount, "source SRS untouched")
            assertEquals(
                SenseOrigin.Service,
                fixture.senseRepository.getById(service.id)?.origin,
                "source sense untouched",
            )
        }
}
