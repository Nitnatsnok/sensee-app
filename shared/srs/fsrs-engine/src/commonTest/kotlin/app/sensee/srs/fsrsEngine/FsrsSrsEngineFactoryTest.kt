package app.sensee.srs.fsrsEngine

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.model.ReviewRating
import app.sensee.srs.core.model.SrsCardState
import app.sensee.srs.engine.SrsReviewRequest
import app.sensee.srs.fsrs.FsrsParameters
import app.sensee.srs.testKit.FixedSrsClock
import app.sensee.srs.testKit.InMemorySrsStorage
import app.sensee.srs.testKit.IncrementalSrsReviewLogIdGenerator
import app.sensee.srs.testKit.SrsTestCards
import app.sensee.srs.testKit.SrsTestInstants
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FsrsSrsEngineFactoryTest {
    @Test
    fun `creates fsrs engine`() =
        runTest {
            val parameters = FsrsParameters.defaultV6()

            val storage =
                InMemorySrsStorage(
                    initialParameters = parameters,
                )

            val cardId = SrsCardId("word-1:recognition")

            storage.saveCard(
                SrsTestCards.newCard(cardId.value),
            )

            val engine =
                FsrsSrsEngineFactory.create(
                    storage = storage,
                    clock = FixedSrsClock(SrsTestInstants.Base),
                    reviewLogIdGenerator = IncrementalSrsReviewLogIdGenerator(),
                )

            engine.submitReview(
                SrsReviewRequest(
                    cardId = cardId,
                    rating = ReviewRating.Good,
                ),
            )

            val savedCard = requireNotNull(storage.getCard(cardId))

            assertEquals(SrsCardState.Learning, savedCard.state)
            assertEquals(1, storage.getReviewLogs(cardId).size)
        }
}
