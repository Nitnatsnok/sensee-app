package app.sensee.srs.fsrsEngine

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.id.SrsReviewLogId
import app.sensee.srs.core.model.ReviewRating
import app.sensee.srs.core.model.SrsCardState
import app.sensee.srs.engine.SrsPreviewRequest
import app.sensee.srs.engine.id.SrsReviewLogIdGenerator
import app.sensee.srs.fsrs.FsrsParameters
import app.sensee.srs.testKit.FixedSrsClock
import app.sensee.srs.testKit.InMemorySrsStorage
import app.sensee.srs.testKit.SrsTestCards
import app.sensee.srs.testKit.SrsTestInstants
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The generic preview matrix (all ratings, no-save, preview-matches-submit, compact options)
 * is already pinned against the real `FsrsScheduler` in `SrsEngineTest`. This test pins the
 * one thing unique to the factory path: an engine built by [FsrsSrsEngineFactory] emits the
 * FSRS learning-step interval from the configured [FsrsParameters], proving the scheduler is
 * wired with its parameters and not a defaulted instance.
 */
class FsrsSrsEngineFactoryPreviewTest {
    private val parameters = FsrsParameters.defaultV6()

    @Test
    fun `preview again option carries the learning interval from the wired fsrs parameters`() =
        runTest {
            val storage = InMemorySrsStorage(initialParameters = parameters)
            val cardId = SrsCardId("word-1:recognition")
            storage.saveCard(SrsTestCards.newCard(cardId.value))

            val engine =
                FsrsSrsEngineFactory.create(
                    storage = storage,
                    clock = FixedSrsClock(SrsTestInstants.Base),
                    reviewLogIdGenerator = FailingReviewLogIdGenerator,
                )

            val again =
                engine
                    .preview(SrsPreviewRequest(cardId = cardId))
                    .get(ReviewRating.Again)
                    .updatedCard

            assertEquals(SrsCardState.Learning, again.state)
            assertEquals(0, again.stepIndex)
            assertEquals(parameters.learningSteps.first(), again.scheduledInterval)
            assertEquals(SrsTestInstants.Base + parameters.learningSteps.first(), again.dueAt)
            assertNotNull(again.algorithmState)
        }

    private object FailingReviewLogIdGenerator : SrsReviewLogIdGenerator {
        override fun nextId(): SrsReviewLogId {
            error("Preview must not generate review log id")
        }
    }
}
