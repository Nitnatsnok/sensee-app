package app.sensee.srs.fsrs

import app.sensee.srs.core.algorithm.SrsSchedulingInput
import app.sensee.srs.core.id.SrsReviewLogId
import app.sensee.srs.core.model.ReviewRating
import app.sensee.srs.core.model.SrsCardSnapshot
import app.sensee.srs.core.model.SrsCardState
import app.sensee.srs.core.model.toReviewSnapshot
import app.sensee.srs.testKit.SrsTestCards
import app.sensee.srs.testKit.SrsTestInstants
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class FsrsSchedulerTest {
    private val scheduler = FsrsScheduler()
    private val parameters = FsrsParameters.defaultV6()
    private val reviewedAt = Instant.parse("2026-04-29T10:00:00Z")

    @Test
    fun `new card with again enters learning`() {
        val result =
            scheduler.schedule(
                input =
                    input(
                        card = newCard(),
                        rating = ReviewRating.Again,
                    ),
            )

        assertEquals(SrsCardState.Learning, result.updatedCard.state)
        assertEquals(0, result.updatedCard.stepIndex)
        assertEquals(parameters.learningSteps.first(), result.updatedCard.scheduledInterval)
        assertNotNull(result.updatedCard.algorithmState as? FsrsAlgorithmState)
    }

    @Test
    fun `new card with good advances through the learning steps`() {
        val result =
            scheduler.schedule(
                input =
                    input(
                        card = newCard(),
                        rating = ReviewRating.Good,
                    ),
            )

        assertEquals(SrsCardState.Learning, result.updatedCard.state)
        assertEquals(1, result.updatedCard.stepIndex)
        assertEquals(parameters.learningSteps[1], result.updatedCard.scheduledInterval)
        assertNotNull(result.updatedCard.algorithmState as? FsrsAlgorithmState)
    }

    @Test
    fun `new card with easy graduates straight to review`() {
        val noFuzzParameters = parameters.copy(enableFuzzing = false)
        val math = FsrsMath(noFuzzParameters)
        val expectedMemoryState = math.initialState(ReviewRating.Easy)
        val expectedInterval = math.nextIntervalDays(expectedMemoryState.stability).days

        val result =
            scheduler.schedule(
                input =
                    input(
                        card = newCard(),
                        rating = ReviewRating.Easy,
                        parameters = noFuzzParameters,
                    ),
            )

        assertEquals(SrsCardState.Review, result.updatedCard.state)
        assertEquals(null, result.updatedCard.stepIndex)
        assertEquals(expectedInterval, result.updatedCard.scheduledInterval)
        assertEquals(reviewedAt + expectedInterval, result.updatedCard.dueAt)
        val memoryState = assertNotNull(result.updatedCard.algorithmState as? FsrsAlgorithmState)
        assertEquals(
            expected = expectedMemoryState.difficulty,
            actual = memoryState.difficulty,
            absoluteTolerance = 1e-9,
        )
        assertEquals(
            expected = expectedMemoryState.stability,
            actual = memoryState.stability,
            absoluteTolerance = 1e-9,
        )
        assertEquals(result.updatedCard.toReviewSnapshot(), result.reviewLog.next)
    }

    @Test
    fun `new card graduates to review after good past the last learning step`() {
        val firstGood =
            scheduler
                .schedule(input(card = newCard(), rating = ReviewRating.Good))
                .updatedCard

        val secondGood =
            scheduler
                .schedule(input(card = firstGood, rating = ReviewRating.Good))
                .updatedCard

        assertEquals(SrsCardState.Learning, firstGood.state)
        assertEquals(SrsCardState.Review, secondGood.state)
        assertEquals(null, secondGood.stepIndex)
    }

    @Test
    fun `review card with again enters relearning`() {
        val card = reviewCard()

        val result =
            scheduler.schedule(
                input =
                    input(
                        card = card,
                        rating = ReviewRating.Again,
                        reviewedAt = Instant.parse("2026-05-05T10:00:00Z"),
                    ),
            )

        assertEquals(SrsCardState.Relearning, result.updatedCard.state)
        assertEquals(0, result.updatedCard.stepIndex)
        assertEquals(1, result.updatedCard.lapseCount)
        assertEquals(parameters.relearningSteps.first(), result.updatedCard.scheduledInterval)
    }

    @Test
    fun `review card on same day uses short term stability`() {
        // ADR-004 keeps this path out of normal session flow, but the math layer must mirror
        // py-fsrs `days_since_last_review < 1 → _short_term_stability`. With defaults and
        // S=6, the short-term SI for Good evaluates < 1 and is clamped to 1.0 ⇒ S stays at 6.
        // The long-term path with elapsed≈1h would instead inflate S to ≈ 6.11.
        val card =
            SrsTestCards.dueReviewCard(
                dueAt = SrsTestInstants.Base + 6.days,
                lastReviewedAt = SrsTestInstants.Base,
                scheduledInterval = 6.days,
                reviewCount = 0,
                algorithmState = FsrsAlgorithmState(difficulty = 5.0, stability = 6.0),
                algorithm = FsrsAlgorithm.V6,
                parametersId = parameters.id,
            )

        val result =
            scheduler.schedule(
                input =
                    input(
                        card = card,
                        rating = ReviewRating.Good,
                        reviewedAt = SrsTestInstants.Base + 1.hours,
                    ),
            )

        val state = assertNotNull(result.updatedCard.algorithmState as? FsrsAlgorithmState)
        assertEquals(
            expected = 6.0,
            actual = state.stability,
            absoluteTolerance = 1e-6,
        )
    }

    @Test
    fun `disabling fuzz yields the deterministic interval from FsrsMath`() {
        // With fuzzing off, the scheduled interval for a Review-state graduate must equal
        // the integer days produced by FsrsMath.nextIntervalDays for the post-review S.
        val noFuzzParameters = parameters.copy(enableFuzzing = false)
        val math = FsrsMath(noFuzzParameters)
        val elapsedDays = 6.0

        val retrievability =
            math.retrievability(
                elapsedDays = elapsedDays,
                stability = 20.0,
            )
        val nextStability =
            math.nextRecallStability(
                difficulty = 5.0,
                stability = 20.0,
                retrievability = retrievability,
                rating = ReviewRating.Good,
            )
        val expectedDays = math.nextIntervalDays(nextStability)

        val card =
            SrsTestCards.dueReviewCard(
                dueAt = SrsTestInstants.Base + 6.days,
                lastReviewedAt = SrsTestInstants.Base,
                scheduledInterval = 6.days,
                reviewCount = 0,
                algorithmState = FsrsAlgorithmState(difficulty = 5.0, stability = 20.0),
                algorithm = FsrsAlgorithm.V6,
                parametersId = noFuzzParameters.id,
            )

        val result =
            scheduler.schedule(
                input =
                    SrsSchedulingInput(
                        card = card,
                        rating = ReviewRating.Good,
                        reviewedAt = SrsTestInstants.Base + 6.days,
                        parameters = noFuzzParameters,
                        reviewLogId = SrsReviewLogId("review-log-no-fuzz"),
                    ),
            )

        assertEquals(expectedDays.days, result.updatedCard.scheduledInterval)
    }

    @Test
    fun `schedule applies fuzz to review intervals and records the fuzzed due date`() {
        val fuzzyParameters = parameters.copy(enableFuzzing = true)
        val scheduler = FsrsScheduler(random = ZeroRandom)
        val reviewedAt = SrsTestInstants.Base + 6.days
        val math = FsrsMath(fuzzyParameters)
        val retrievability =
            math.retrievability(
                elapsedDays = 6.0,
                stability = 20.0,
            )
        val nextStability =
            math.nextRecallStability(
                difficulty = 5.0,
                stability = 20.0,
                retrievability = retrievability,
                rating = ReviewRating.Good,
            )
        val deterministicDays = math.nextIntervalDays(nextStability)
        val expectedFuzzedDays =
            fuzzedIntervalDays(
                intervalDays = deterministicDays,
                maximumIntervalDays = fuzzyParameters.maximumIntervalDays,
                random = ZeroRandom,
            )
        val card =
            SrsTestCards.dueReviewCard(
                dueAt = reviewedAt,
                lastReviewedAt = SrsTestInstants.Base,
                scheduledInterval = 6.days,
                reviewCount = 0,
                algorithmState = FsrsAlgorithmState(difficulty = 5.0, stability = 20.0),
                algorithm = FsrsAlgorithm.V6,
                parametersId = fuzzyParameters.id,
            )

        val result =
            scheduler.schedule(
                input =
                    SrsSchedulingInput(
                        card = card,
                        rating = ReviewRating.Good,
                        reviewedAt = reviewedAt,
                        parameters = fuzzyParameters,
                        reviewLogId = SrsReviewLogId("review-log-fuzz"),
                    ),
            )

        assertTrue(
            actual = expectedFuzzedDays < deterministicDays,
            message = "test setup must pick a fuzzed interval below the deterministic interval",
        )
        assertEquals(expectedFuzzedDays.days, result.updatedCard.scheduledInterval)
        assertEquals(reviewedAt + expectedFuzzedDays.days, result.updatedCard.dueAt)
        assertEquals(result.updatedCard.toReviewSnapshot(), result.reviewLog.next)
    }

    @Test
    fun `review card with good stays in review`() {
        val noFuzzParameters = parameters.copy(enableFuzzing = false)
        val reviewedAt = Instant.parse("2026-05-05T10:00:00Z")
        val math = FsrsMath(noFuzzParameters)
        val retrievability =
            math.retrievability(
                elapsedDays = 6.0,
                stability = 6.0,
            )
        val expectedDifficulty =
            math.nextDifficulty(
                currentDifficulty = 5.0,
                rating = ReviewRating.Good,
            )
        val expectedStability =
            math.nextRecallStability(
                difficulty = 5.0,
                stability = 6.0,
                retrievability = retrievability,
                rating = ReviewRating.Good,
            )
        val expectedInterval = math.nextIntervalDays(expectedStability).days
        val card = reviewCard(parameters = noFuzzParameters)

        val result =
            scheduler.schedule(
                input =
                    input(
                        card = card,
                        rating = ReviewRating.Good,
                        reviewedAt = reviewedAt,
                        parameters = noFuzzParameters,
                    ),
            )

        assertEquals(SrsCardState.Review, result.updatedCard.state)
        assertEquals(null, result.updatedCard.stepIndex)
        assertEquals(1, result.updatedCard.reviewCount)
        assertEquals(0, result.updatedCard.lapseCount)
        assertEquals(expectedInterval, result.updatedCard.scheduledInterval)
        assertEquals(reviewedAt + expectedInterval, result.updatedCard.dueAt)
        val memoryState = assertNotNull(result.updatedCard.algorithmState as? FsrsAlgorithmState)
        assertEquals(
            expected = expectedDifficulty,
            actual = memoryState.difficulty,
            absoluteTolerance = 1e-9,
        )
        assertEquals(
            expected = expectedStability,
            actual = memoryState.stability,
            absoluteTolerance = 1e-9,
        )
        assertEquals(card.toReviewSnapshot(), result.reviewLog.previous)
        assertEquals(result.updatedCard.toReviewSnapshot(), result.reviewLog.next)
    }

    private fun input(
        card: SrsCardSnapshot,
        rating: ReviewRating,
        reviewedAt: Instant = this.reviewedAt,
        parameters: FsrsParameters = this.parameters,
    ): SrsSchedulingInput<FsrsParameters> =
        SrsSchedulingInput(
            card = card,
            rating = rating,
            reviewedAt = reviewedAt,
            parameters = parameters,
            reviewLogId = SrsReviewLogId("review-log-1"),
        )

    private fun newCard(): SrsCardSnapshot = SrsTestCards.newCard()

    private fun reviewCard(parameters: FsrsParameters = this.parameters): SrsCardSnapshot =
        SrsTestCards.dueReviewCard(
            dueAt = Instant.parse("2026-05-05T10:00:00Z"),
            lastReviewedAt = SrsTestInstants.Base,
            scheduledInterval = 6.days,
            reviewCount = 0,
            algorithmState = FsrsAlgorithmState(difficulty = 5.0, stability = 6.0),
            algorithm = FsrsAlgorithm.V6,
            parametersId = parameters.id,
        )

    private object ZeroRandom : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }
}
