package app.sensee.srs.engine

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.id.SrsReviewLogId
import app.sensee.srs.core.model.ReviewRating
import app.sensee.srs.core.model.SrsCardState
import app.sensee.srs.engine.id.SrsReviewLogIdGenerator
import app.sensee.srs.fsrs.FsrsAlgorithmState
import app.sensee.srs.fsrs.FsrsParameters
import app.sensee.srs.fsrs.FsrsScheduler
import app.sensee.srs.testKit.FixedSrsClock
import app.sensee.srs.testKit.InMemorySrsStorage
import app.sensee.srs.testKit.IncrementalSrsReviewLogIdGenerator
import app.sensee.srs.testKit.SrsTestCards
import app.sensee.srs.testKit.SrsTestInstants
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class SrsEngineTest {
    private val parameters = FsrsParameters.defaultV6()

    @Test
    fun `submit review updates card and appends review log`() =
        runTest {
            val storage =
                InMemorySrsStorage(
                    initialParameters = parameters,
                )

            val cardId = SrsCardId("word-1:recognition")

            storage.saveCard(
                SrsTestCards.newCard(cardId.value),
            )

            val engine =
                createEngine(
                    storage = storage,
                )

            val result =
                engine.submitReview(
                    SrsReviewRequest(
                        cardId = cardId,
                        rating = ReviewRating.Good,
                    ),
                )

            val savedCard = requireNotNull(storage.getCard(cardId))
            val logs = storage.getReviewLogs(cardId)

            assertEquals(result.updatedCard, savedCard)
            assertEquals(SrsCardState.Learning, savedCard.state)
            assertEquals(1, savedCard.reviewCount)
            assertEquals(0, savedCard.lapseCount)
            assertEquals(1, savedCard.stepIndex)
            assertEquals(parameters.id, savedCard.parametersId)
            assertNotNull(savedCard.algorithmState as? FsrsAlgorithmState)

            assertEquals(1, logs.size)

            val log = logs.first()

            assertEquals(SrsReviewLogId("review-log-1"), log.id)
            assertEquals(cardId, log.cardId)
            assertEquals(ReviewRating.Good, log.rating)
            assertEquals(SrsTestInstants.Base, log.reviewedAt)
            assertEquals(SrsCardState.New, log.previous.state)
            assertEquals(SrsCardState.Learning, log.next.state)
            assertEquals(parameters.id, log.parametersId)
        }

    @Test
    fun `new card with again enters learning state`() =
        runTest {
            val storage =
                InMemorySrsStorage(
                    initialParameters = parameters,
                )

            val cardId = SrsCardId("word-1:recognition")

            storage.saveCard(
                SrsTestCards.newCard(cardId.value),
            )

            val engine =
                createEngine(
                    storage = storage,
                )

            val result =
                engine.submitReview(
                    SrsReviewRequest(
                        cardId = cardId,
                        rating = ReviewRating.Again,
                    ),
                )

            val savedCard = requireNotNull(storage.getCard(cardId))
            val logs = storage.getReviewLogs(cardId)

            assertEquals(result.updatedCard, savedCard)
            assertEquals(SrsCardState.Learning, savedCard.state)
            assertEquals(0, savedCard.stepIndex)
            assertEquals(parameters.learningSteps.first(), savedCard.scheduledInterval)
            assertEquals(SrsTestInstants.Base + parameters.learningSteps.first(), savedCard.dueAt)
            assertEquals(1, savedCard.reviewCount)
            assertEquals(0, savedCard.lapseCount)
            assertNotNull(savedCard.algorithmState as? FsrsAlgorithmState)

            assertEquals(1, logs.size)
            assertEquals(SrsCardState.New, logs.first().previous.state)
            assertEquals(SrsCardState.Learning, logs.first().next.state)
        }

    @Test
    fun `review card with again enters relearning and increments lapse count`() =
        runTest {
            val storage =
                InMemorySrsStorage(
                    initialParameters = parameters,
                )

            val cardId = SrsCardId("word-1:recognition")

            storage.saveCard(
                SrsTestCards.dueReviewCard(
                    id = cardId.value,
                    dueAt = SrsTestInstants.Base,
                    lastReviewedAt = SrsTestInstants.Base - 6.days,
                    scheduledInterval = 6.days,
                    reviewCount = 3,
                    lapseCount = 0,
                    algorithmState =
                        FsrsAlgorithmState(
                            difficulty = 5.0,
                            stability = 6.0,
                        ),
                    algorithm = parameters.algorithm,
                    parametersId = parameters.id,
                ),
            )

            val engine =
                createEngine(
                    storage = storage,
                )

            val result =
                engine.submitReview(
                    SrsReviewRequest(
                        cardId = cardId,
                        rating = ReviewRating.Again,
                    ),
                )

            val savedCard = requireNotNull(storage.getCard(cardId))
            val logs = storage.getReviewLogs(cardId)

            assertEquals(result.updatedCard, savedCard)
            assertEquals(SrsCardState.Relearning, savedCard.state)
            assertEquals(0, savedCard.stepIndex)
            assertEquals(4, savedCard.reviewCount)
            assertEquals(1, savedCard.lapseCount)
            assertEquals(parameters.relearningSteps.first(), savedCard.scheduledInterval)
            assertEquals(SrsTestInstants.Base + parameters.relearningSteps.first(), savedCard.dueAt)
            assertNotNull(savedCard.algorithmState as? FsrsAlgorithmState)

            assertEquals(1, logs.size)
            assertEquals(SrsCardState.Review, logs.first().previous.state)
            assertEquals(SrsCardState.Relearning, logs.first().next.state)
        }

    @Test
    fun `review card with good stays in review`() =
        runTest {
            val storage =
                InMemorySrsStorage(
                    initialParameters = parameters,
                )

            val cardId = SrsCardId("word-1:recognition")

            storage.saveCard(
                SrsTestCards.dueReviewCard(
                    id = cardId.value,
                    dueAt = SrsTestInstants.Base,
                    lastReviewedAt = SrsTestInstants.Base - 6.days,
                    scheduledInterval = 6.days,
                    reviewCount = 3,
                    lapseCount = 0,
                    algorithmState =
                        FsrsAlgorithmState(
                            difficulty = 5.0,
                            stability = 6.0,
                        ),
                    algorithm = parameters.algorithm,
                    parametersId = parameters.id,
                ),
            )

            val engine =
                createEngine(
                    storage = storage,
                )

            val result =
                engine.submitReview(
                    SrsReviewRequest(
                        cardId = cardId,
                        rating = ReviewRating.Good,
                    ),
                )

            val savedCard = requireNotNull(storage.getCard(cardId))
            val logs = storage.getReviewLogs(cardId)

            assertEquals(result.updatedCard, savedCard)
            assertEquals(SrsCardState.Review, savedCard.state)
            assertNull(savedCard.stepIndex)
            assertEquals(4, savedCard.reviewCount)
            assertEquals(0, savedCard.lapseCount)
            assertNotNull(savedCard.dueAt)
            assertNotNull(savedCard.scheduledInterval)
            assertTrue(savedCard.scheduledInterval!! >= 1.days)
            assertNotNull(savedCard.algorithmState as? FsrsAlgorithmState)

            assertEquals(1, logs.size)
            assertEquals(ReviewRating.Good, logs.first().rating)
            assertEquals(SrsCardState.Review, logs.first().previous.state)
            assertEquals(SrsCardState.Review, logs.first().next.state)
        }

    @Test
    fun `explicit reviewedAt overrides clock time`() =
        runTest {
            val storage =
                InMemorySrsStorage(
                    initialParameters = parameters,
                )

            val cardId = SrsCardId("word-1:recognition")
            val explicitReviewedAt = Instant.parse("2026-05-10T12:30:00Z")

            storage.saveCard(
                SrsTestCards.newCard(cardId.value),
            )

            val engine =
                createEngine(
                    storage = storage,
                    clock = FixedSrsClock(SrsTestInstants.Base),
                )

            engine.submitReview(
                SrsReviewRequest(
                    cardId = cardId,
                    rating = ReviewRating.Good,
                    reviewedAt = explicitReviewedAt,
                ),
            )

            val savedCard = requireNotNull(storage.getCard(cardId))
            val logs = storage.getReviewLogs(cardId)

            assertEquals(explicitReviewedAt, savedCard.lastReviewedAt)
            assertEquals(explicitReviewedAt, logs.first().reviewedAt)
        }

    @Test
    fun `throws when card does not exist`() =
        runTest {
            val storage =
                InMemorySrsStorage(
                    initialParameters = parameters,
                )

            val engine =
                createEngine(
                    storage = storage,
                )

            val exception =
                assertFailsWith<SrsCardNotFoundException> {
                    engine.submitReview(
                        SrsReviewRequest(
                            cardId = SrsCardId("missing-card"),
                            rating = ReviewRating.Good,
                        ),
                    )
                }

            assertEquals(SrsCardId("missing-card"), exception.cardId)
            assertTrue(storage.getAllReviewLogs().isEmpty())
        }

    @Test
    fun `get due cards returns cards scheduled before or at now`() =
        runTest {
            val storage =
                InMemorySrsStorage(
                    initialParameters = parameters,
                )

            val dueCardId = SrsCardId("word-1:recognition")
            val futureCardId = SrsCardId("word-2:recognition")
            val suspendedCardId = SrsCardId("word-3:recognition")

            storage.saveCard(
                SrsTestCards.dueReviewCard(
                    id = dueCardId.value,
                    dueAt = SrsTestInstants.Base,
                    lastReviewedAt = SrsTestInstants.Base - 1.days,
                    scheduledInterval = 1.days,
                    algorithmState =
                        FsrsAlgorithmState(
                            difficulty = 5.0,
                            stability = 1.0,
                        ),
                    algorithm = parameters.algorithm,
                    parametersId = parameters.id,
                ),
            )

            storage.saveCard(
                SrsTestCards.dueReviewCard(
                    id = futureCardId.value,
                    dueAt = SrsTestInstants.AfterThreeDays,
                    lastReviewedAt = SrsTestInstants.Base,
                    scheduledInterval = 3.days,
                    algorithmState =
                        FsrsAlgorithmState(
                            difficulty = 5.0,
                            stability = 3.0,
                        ),
                    algorithm = parameters.algorithm,
                    parametersId = parameters.id,
                ),
            )

            storage.saveCard(
                SrsTestCards
                    .suspendedCard(
                        id = suspendedCardId.value,
                    ).copy(
                        dueAt = SrsTestInstants.Base,
                    ),
            )

            val dueCards =
                storage.getDueCards(
                    now = SrsTestInstants.Base,
                    limit = 10,
                )

            assertEquals(listOf(dueCardId), dueCards.map { it.id })
        }

    @Test
    fun `review log ids are generated incrementally`() =
        runTest {
            val storage =
                InMemorySrsStorage(
                    initialParameters = parameters,
                )

            val firstCardId = SrsCardId("word-1:recognition")
            val secondCardId = SrsCardId("word-2:recognition")

            storage.saveCard(
                SrsTestCards.newCard(firstCardId.value),
            )

            storage.saveCard(
                SrsTestCards.newCard(secondCardId.value),
            )

            val engine =
                createEngine(
                    storage = storage,
                    reviewLogIdGenerator =
                        IncrementalSrsReviewLogIdGenerator(
                            prefix = "test-log",
                        ),
                )

            engine.submitReview(
                SrsReviewRequest(
                    cardId = firstCardId,
                    rating = ReviewRating.Good,
                ),
            )

            engine.submitReview(
                SrsReviewRequest(
                    cardId = secondCardId,
                    rating = ReviewRating.Good,
                ),
            )

            val allLogs = storage.getAllReviewLogs()

            assertEquals(2, allLogs.size)
            assertEquals(SrsReviewLogId("test-log-1"), allLogs[0].id)
            assertEquals(SrsReviewLogId("test-log-2"), allLogs[1].id)
        }

    @Test
    fun `preview returns all rating options without saving card or appending log`() =
        runTest {
            val storage =
                InMemorySrsStorage(
                    initialParameters = parameters,
                )

            val cardId = SrsCardId("word-1:recognition")
            val initialCard = SrsTestCards.newCard(cardId.value)

            storage.saveCard(initialCard)

            val engine =
                SrsEngineFactory.create(
                    scheduler = FsrsScheduler(),
                    storage = storage,
                    clock = FixedSrsClock(SrsTestInstants.Base),
                    reviewLogIdGenerator = FailingReviewLogIdGenerator,
                )

            val preview =
                engine.preview(
                    SrsPreviewRequest(
                        cardId = cardId,
                    ),
                )

            val savedCard = storage.getCard(cardId)
            val logs = storage.getReviewLogs(cardId)

            assertEquals(initialCard, savedCard)
            assertTrue(logs.isEmpty())

            assertEquals(initialCard, preview.sourceCard)
            assertEquals(SrsTestInstants.Base, preview.reviewedAt)

            assertEquals(
                ReviewRating.entries.toSet(),
                preview.ratings.map { it.rating }.toSet(),
            )

            assertEquals(SrsCardState.Learning, preview.get(ReviewRating.Again).updatedCard.state)
            assertEquals(SrsCardState.Learning, preview.get(ReviewRating.Hard).updatedCard.state)
            assertEquals(SrsCardState.Learning, preview.get(ReviewRating.Good).updatedCard.state)
            assertEquals(SrsCardState.Review, preview.get(ReviewRating.Easy).updatedCard.state)
        }

    @Test
    fun `preview for rating matches submit review result`() =
        runTest {
            val storage =
                InMemorySrsStorage(
                    initialParameters = parameters,
                )

            val cardId = SrsCardId("word-1:recognition")

            storage.saveCard(
                SrsTestCards.newCard(cardId.value),
            )

            val engine =
                createEngine(
                    storage = storage,
                )

            val preview =
                engine.preview(
                    SrsPreviewRequest(
                        cardId = cardId,
                    ),
                )

            val submitResult =
                engine.submitReview(
                    SrsReviewRequest(
                        cardId = cardId,
                        rating = ReviewRating.Good,
                    ),
                )

            assertEquals(
                preview.get(ReviewRating.Good).updatedCard,
                submitResult.updatedCard,
            )
        }

    @Test
    fun `preview options returns compact options without saving card or appending log`() =
        runTest {
            val storage =
                InMemorySrsStorage(
                    initialParameters = parameters,
                )

            val cardId = SrsCardId("word-1:recognition")
            val initialCard = SrsTestCards.newCard(cardId.value)

            storage.saveCard(initialCard)

            val engine =
                createEngine(
                    storage = storage,
                    reviewLogIdGenerator = FailingReviewLogIdGenerator,
                )

            val options =
                engine.previewOptions(
                    SrsPreviewRequest(
                        cardId = cardId,
                    ),
                )

            assertEquals(cardId, options.cardId)
            assertEquals(SrsTestInstants.Base, options.reviewedAt)

            assertEquals(
                ReviewRating.entries.toSet(),
                options.options.map { it.rating }.toSet(),
            )

            val again = options.get(ReviewRating.Again)
            val hard = options.get(ReviewRating.Hard)
            val good = options.get(ReviewRating.Good)
            val easy = options.get(ReviewRating.Easy)

            assertEquals(SrsCardState.Learning, again.state)
            assertEquals(SrsCardState.Learning, hard.state)
            assertEquals(SrsCardState.Learning, good.state)
            assertEquals(SrsCardState.Review, easy.state)

            assertEquals(initialCard, storage.getCard(cardId))
            assertTrue(storage.getReviewLogs(cardId).isEmpty())
        }

    private fun createEngine(
        storage: InMemorySrsStorage<FsrsParameters>,
        clock: FixedSrsClock = FixedSrsClock(SrsTestInstants.Base),
        reviewLogIdGenerator: SrsReviewLogIdGenerator =
            IncrementalSrsReviewLogIdGenerator(),
    ): SrsEngine<FsrsParameters> =
        SrsEngineFactory.create(
            scheduler = FsrsScheduler(),
            storage = storage,
            clock = clock,
            reviewLogIdGenerator = reviewLogIdGenerator,
        )

    private object FailingReviewLogIdGenerator : SrsReviewLogIdGenerator {
        override fun nextId(): SrsReviewLogId {
            error("Preview must not generate review log id")
        }
    }
}
