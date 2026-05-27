package app.sensee.srs.fsrs

import app.sensee.srs.core.algorithm.SrsSchedulingInput
import app.sensee.srs.core.id.SrsReviewLogId
import app.sensee.srs.core.model.ReviewRating
import app.sensee.srs.core.model.SrsCardSnapshot
import app.sensee.srs.core.model.SrsCardState
import app.sensee.srs.testKit.SrsTestCards
import app.sensee.srs.testKit.SrsTestInstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
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
        val result =
            scheduler.schedule(
                input =
                    input(
                        card = newCard(),
                        rating = ReviewRating.Easy,
                    ),
            )

        assertEquals(SrsCardState.Review, result.updatedCard.state)
        assertEquals(null, result.updatedCard.stepIndex)
        assertTrue(result.updatedCard.scheduledInterval!! >= 1.minutes)
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
    fun `review card with good stays in review`() {
        val card = reviewCard()

        val result =
            scheduler.schedule(
                input =
                    input(
                        card = card,
                        rating = ReviewRating.Good,
                        reviewedAt = Instant.parse("2026-05-05T10:00:00Z"),
                    ),
            )

        assertEquals(SrsCardState.Review, result.updatedCard.state)
        assertEquals(null, result.updatedCard.stepIndex)
        assertEquals(1, result.updatedCard.reviewCount)
        assertEquals(0, result.updatedCard.lapseCount)
        assertNotNull(result.updatedCard.scheduledInterval)
    }

    private fun input(
        card: SrsCardSnapshot,
        rating: ReviewRating,
        reviewedAt: Instant = this.reviewedAt,
    ): SrsSchedulingInput<FsrsParameters> =
        SrsSchedulingInput(
            card = card,
            rating = rating,
            reviewedAt = reviewedAt,
            parameters = parameters,
            reviewLogId = SrsReviewLogId("review-log-1"),
        )

    private fun newCard(): SrsCardSnapshot = SrsTestCards.newCard()

    private fun reviewCard(): SrsCardSnapshot =
        SrsTestCards.dueReviewCard(
            dueAt = Instant.parse("2026-05-05T10:00:00Z"),
            lastReviewedAt = SrsTestInstants.Base,
            scheduledInterval = 6.days,
            reviewCount = 0,
            algorithmState = FsrsAlgorithmState(difficulty = 5.0, stability = 6.0),
            algorithm = FsrsAlgorithm.V6,
            parametersId = parameters.id,
        )
}
