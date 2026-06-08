package app.sensee.feature.practice.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.practice.data.local.PracticeSrsStorage
import app.sensee.feature.practice.domain.CardReview
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.id.SrsReviewLogId
import app.sensee.srs.core.model.ReviewRating
import app.sensee.srs.core.model.SrsCardState
import app.sensee.srs.engine.clock.SrsClock
import app.sensee.srs.engine.id.SrsReviewLogIdGenerator
import app.sensee.srs.fsrsEngine.FsrsSrsEngineFactory
import kotlinx.coroutines.test.runTest
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class DefaultPracticeReviewRepositoryTest {
    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    private class IncrementingReviewLogIdGenerator : SrsReviewLogIdGenerator {
        private var next = 0

        override fun nextId(): SrsReviewLogId = SrsReviewLogId("review-log-${++next}")
    }

    @Test
    fun `submit review materializes a missing SRS card before scheduling`() =
        runTest {
            val storage = PracticeSrsStorage(FakeDbProvider(freshDatabase()))
            val engine =
                FsrsSrsEngineFactory.create(
                    storage = storage,
                    clock = SrsClock { Instant.parse("2026-05-19T10:00:00Z") },
                    reviewLogIdGenerator = IncrementingReviewLogIdGenerator(),
                )
            val repository =
                DefaultPracticeReviewRepository(
                    srsStorage = storage,
                    srsEngine = engine,
                )
            val cardId = SrsCardId("captured-sense")

            assertNull(storage.getCard(cardId))

            val outcome =
                repository.submitReview(
                    CardReview(
                        cardId = cardId,
                        rating = ReviewRating.Good,
                    ),
                )

            val persisted = assertNotNull(storage.getCard(cardId))
            assertEquals(outcome.srs, persisted)
            assertEquals(SrsCardState.Learning, persisted.state)
            assertEquals(1, persisted.reviewCount)
        }

    private fun freshDatabase(): SenseeDatabase {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY,
                Properties(),
                SenseeDatabase.Schema.synchronous(),
            )
        return SenseeDatabase(driver)
    }
}
