package app.sensee.srs.fsrs

import app.sensee.srs.core.algorithm.SrsRatingPreview
import app.sensee.srs.core.algorithm.SrsScheduler
import app.sensee.srs.core.algorithm.SrsSchedulingInput
import app.sensee.srs.core.algorithm.SrsSchedulingPreview
import app.sensee.srs.core.algorithm.SrsSchedulingPreviewInput
import app.sensee.srs.core.algorithm.SrsSchedulingResult
import app.sensee.srs.core.log.SrsReviewLog
import app.sensee.srs.core.model.ReviewRating
import app.sensee.srs.core.model.SrsAlgorithmInfo
import app.sensee.srs.core.model.SrsCardSnapshot
import app.sensee.srs.core.model.SrsCardState
import app.sensee.srs.core.model.toReviewSnapshot
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

public class FsrsScheduler(
    private val random: Random = Random.Default,
) : SrsScheduler<FsrsParameters> {
    override val algorithm: SrsAlgorithmInfo = FsrsAlgorithm.V6

    override fun schedule(input: SrsSchedulingInput<FsrsParameters>): SrsSchedulingResult {
        val math = FsrsMath(input.parameters)
        val previousCard = input.card

        val scheduledCard =
            scheduleCard(
                card = previousCard,
                rating = input.rating,
                reviewedAt = input.reviewedAt,
                parameters = input.parameters,
                math = math,
            )

        val updatedCard =
            applyIntervalFuzzIfNeeded(
                card = scheduledCard,
                reviewedAt = input.reviewedAt,
                parameters = input.parameters,
            )

        val reviewLog =
            SrsReviewLog(
                id = input.reviewLogId,
                cardId = previousCard.id,
                rating = input.rating,
                reviewedAt = input.reviewedAt,
                previous = previousCard.toReviewSnapshot(),
                next = updatedCard.toReviewSnapshot(),
                algorithm = algorithm,
                parametersId = input.parameters.id,
            )

        return SrsSchedulingResult(
            updatedCard = updatedCard,
            reviewLog = reviewLog,
        )
    }

    override fun preview(input: SrsSchedulingPreviewInput<FsrsParameters>): SrsSchedulingPreview {
        val math = FsrsMath(input.parameters)

        val ratings =
            ReviewRating.entries.map { rating ->
                val updatedCard =
                    scheduleCard(
                        card = input.card,
                        rating = rating,
                        reviewedAt = input.reviewedAt,
                        parameters = input.parameters,
                        math = math,
                    )

                SrsRatingPreview(
                    rating = rating,
                    updatedCard = updatedCard,
                )
            }

        return SrsSchedulingPreview(
            sourceCard = input.card,
            reviewedAt = input.reviewedAt,
            ratings = ratings,
        )
    }

    /**
     * Applies py-fsrs-style interval jitter to a freshly scheduled Review card.
     *
     * Fuzz is intentionally skipped for `preview()` so that the rating tiles a user sees in
     * the UI stay deterministic between renders — only the actual `schedule()` call rolls
     * the dice. Non-Review states and disabled-fuzzing parameters are passed through.
     */
    private fun applyIntervalFuzzIfNeeded(
        card: SrsCardSnapshot,
        reviewedAt: Instant,
        parameters: FsrsParameters,
    ): SrsCardSnapshot {
        if (!parameters.enableFuzzing) return card
        if (card.state != SrsCardState.Review) return card
        val originalInterval = card.scheduledInterval ?: return card

        val originalDays = originalInterval.inWholeDays.toInt()
        val fuzzedDays =
            fuzzedIntervalDays(
                intervalDays = originalDays,
                maximumIntervalDays = parameters.maximumIntervalDays,
                random = random,
            )
        if (fuzzedDays == originalDays) return card

        val fuzzedInterval = fuzzedDays.days
        return card.copy(
            scheduledInterval = fuzzedInterval,
            dueAt = reviewedAt + fuzzedInterval,
        )
    }

    private fun scheduleCard(
        card: SrsCardSnapshot,
        rating: ReviewRating,
        reviewedAt: Instant,
        parameters: FsrsParameters,
        math: FsrsMath,
    ): SrsCardSnapshot {
        val input =
            InternalSchedulingInput(
                card = card,
                rating = rating,
                reviewedAt = reviewedAt,
                parameters = parameters,
            )

        return when (card.state) {
            // A New card's first answer runs through the learning steps just like a Learning
            // card (FSRS-with-learning-steps / Anki behavior): Again/Hard/Good keep it in the
            // short-term loop and only Good-past-the-last-step or Easy graduates it. Sending
            // New+Good straight to Review would skip the steps and schedule a brand-new card
            // days out after a single correct answer.
            SrsCardState.New,
            SrsCardState.Learning,
            ->
                scheduleLearningCard(
                    input = input,
                    math = math,
                    steps = parameters.learningSteps,
                )

            SrsCardState.Review ->
                scheduleReviewCard(
                    input = input,
                    math = math,
                )

            SrsCardState.Relearning ->
                scheduleLearningCard(
                    input = input,
                    math = math,
                    steps = parameters.relearningSteps,
                )

            SrsCardState.Suspended -> card
        }
    }

    private fun scheduleLearningCard(
        input: InternalSchedulingInput,
        math: FsrsMath,
        steps: List<Duration>,
    ): SrsCardSnapshot {
        val card = input.card
        val rating = input.rating
        val parameters = input.parameters

        val currentMemoryState = card.algorithmState as? FsrsAlgorithmState
        val nextMemoryState =
            nextMemoryStateForLearning(
                card = card,
                rating = rating,
                reviewedAt = input.reviewedAt,
                math = math,
                currentMemoryState = currentMemoryState,
            )

        val currentStepIndex = card.stepIndex ?: 0

        val next =
            if (steps.isEmpty()) {
                ScheduledCardData(
                    state = SrsCardState.Review,
                    stepIndex = null,
                    interval = math.nextIntervalDays(nextMemoryState.stability).days,
                    memoryState = nextMemoryState,
                )
            } else {
                scheduleLearningStep(
                    rating = rating,
                    currentStepIndex = currentStepIndex,
                    steps = steps,
                    nextReviewInterval = math.nextIntervalDays(nextMemoryState.stability).days,
                    memoryState = nextMemoryState,
                )
            }

        return card.copy(
            state = next.state,
            dueAt = input.reviewedAt + next.interval,
            lastReviewedAt = input.reviewedAt,
            scheduledInterval = next.interval,
            reviewCount = card.reviewCount + 1,
            lapseCount = card.lapseCount,
            stepIndex = next.stepIndex,
            algorithmState = next.memoryState,
            algorithm = algorithm,
            parametersId = parameters.id,
        )
    }

    private fun scheduleReviewCard(
        input: InternalSchedulingInput,
        math: FsrsMath,
    ): SrsCardSnapshot {
        val card = input.card
        val parameters = input.parameters

        val currentMemoryState =
            requireNotNull(card.algorithmState as? FsrsAlgorithmState) {
                "Review card must have FsrsAlgorithmState"
            }

        val elapsedDays =
            elapsedDays(
                lastReviewedAt = card.lastReviewedAt,
                reviewedAt = input.reviewedAt,
            )
        val nextMemoryState =
            nextMemoryStateForReview(
                math = math,
                currentMemoryState = currentMemoryState,
                rating = input.rating,
                elapsedDays = elapsedDays,
            )
        val next =
            scheduledReviewCardData(
                input = input,
                math = math,
                nextMemoryState = nextMemoryState,
            )

        return card.copy(
            state = next.state,
            dueAt = input.reviewedAt + next.interval,
            lastReviewedAt = input.reviewedAt,
            scheduledInterval = next.interval,
            reviewCount = card.reviewCount + 1,
            lapseCount =
                if (input.rating == ReviewRating.Again) {
                    card.lapseCount + 1
                } else {
                    card.lapseCount
                },
            stepIndex = next.stepIndex,
            algorithmState = next.memoryState,
            algorithm = algorithm,
            parametersId = parameters.id,
        )
    }

    private fun nextMemoryStateForLearning(
        card: SrsCardSnapshot,
        rating: ReviewRating,
        reviewedAt: Instant,
        math: FsrsMath,
        currentMemoryState: FsrsAlgorithmState?,
    ): FsrsAlgorithmState {
        if (currentMemoryState == null || card.lastReviewedAt == null) {
            return math.initialState(rating)
        }

        val elapsedDays =
            elapsedDays(
                lastReviewedAt = card.lastReviewedAt,
                reviewedAt = reviewedAt,
            )

        val nextDifficulty =
            math.nextDifficulty(
                currentDifficulty = currentMemoryState.difficulty,
                rating = rating,
            )

        val nextStability =
            nextLearningStability(
                math = math,
                currentMemoryState = currentMemoryState,
                rating = rating,
                elapsedDays = elapsedDays,
            )

        return FsrsAlgorithmState(
            difficulty = nextDifficulty,
            stability = nextStability,
        )
    }

    private fun nextLearningStability(
        math: FsrsMath,
        currentMemoryState: FsrsAlgorithmState,
        rating: ReviewRating,
        elapsedDays: Double,
    ): Double {
        if (elapsedDays < 1.0) {
            return math.nextShortTermStability(
                currentStability = currentMemoryState.stability,
                rating = rating,
            )
        }

        val retrievability =
            math.retrievability(
                elapsedDays = elapsedDays,
                stability = currentMemoryState.stability,
            )

        return when (rating) {
            ReviewRating.Again ->
                math.nextForgetStability(
                    difficulty = currentMemoryState.difficulty,
                    stability = currentMemoryState.stability,
                    retrievability = retrievability,
                )

            ReviewRating.Hard,
            ReviewRating.Good,
            ReviewRating.Easy,
            ->
                math.nextRecallStability(
                    difficulty = currentMemoryState.difficulty,
                    stability = currentMemoryState.stability,
                    retrievability = retrievability,
                    rating = rating,
                )
        }
    }

    private fun scheduleLearningStep(
        rating: ReviewRating,
        currentStepIndex: Int,
        steps: List<Duration>,
        nextReviewInterval: Duration,
        memoryState: FsrsAlgorithmState,
    ): ScheduledCardData =
        when (rating) {
            ReviewRating.Again ->
                ScheduledCardData(
                    state = SrsCardState.Learning,
                    stepIndex = 0,
                    interval = steps[0],
                    memoryState = memoryState,
                )

            ReviewRating.Hard ->
                ScheduledCardData(
                    state = SrsCardState.Learning,
                    stepIndex = currentStepIndex.coerceAtMost(steps.lastIndex),
                    interval =
                        hardIntervalForStep(
                            currentStepIndex = currentStepIndex,
                            steps = steps,
                        ),
                    memoryState = memoryState,
                )

            ReviewRating.Good -> {
                val nextStepIndex = currentStepIndex + 1

                if (nextStepIndex > steps.lastIndex) {
                    ScheduledCardData(
                        state = SrsCardState.Review,
                        stepIndex = null,
                        interval = nextReviewInterval,
                        memoryState = memoryState,
                    )
                } else {
                    ScheduledCardData(
                        state = SrsCardState.Learning,
                        stepIndex = nextStepIndex,
                        interval = steps[nextStepIndex],
                        memoryState = memoryState,
                    )
                }
            }

            ReviewRating.Easy ->
                ScheduledCardData(
                    state = SrsCardState.Review,
                    stepIndex = null,
                    interval = nextReviewInterval,
                    memoryState = memoryState,
                )
        }

    private fun hardIntervalForStep(
        currentStepIndex: Int,
        steps: List<Duration>,
    ): Duration {
        val safeStepIndex = currentStepIndex.coerceAtMost(steps.lastIndex)

        return when {
            safeStepIndex == 0 && steps.size == 1 -> steps[0] * 1.5
            safeStepIndex == 0 && steps.size >= 2 -> (steps[0] + steps[1]) / 2
            else -> steps[safeStepIndex]
        }
    }

    private fun elapsedDays(
        lastReviewedAt: Instant?,
        reviewedAt: Instant,
    ): Double {
        if (lastReviewedAt == null) return 0.0

        val duration = reviewedAt - lastReviewedAt

        return duration.inWholeMilliseconds
            .coerceAtLeast(0L)
            .toDouble() / MILLIS_PER_DAY
    }

    private companion object {
        const val MILLIS_PER_DAY: Double = 86_400_000.0
    }
}

private data class ScheduledCardData(
    val state: SrsCardState,
    val stepIndex: Int?,
    val interval: Duration,
    val memoryState: FsrsAlgorithmState,
)

private data class InternalSchedulingInput(
    val card: SrsCardSnapshot,
    val rating: ReviewRating,
    val reviewedAt: Instant,
    val parameters: FsrsParameters,
)

private fun nextMemoryStateForReview(
    math: FsrsMath,
    currentMemoryState: FsrsAlgorithmState,
    rating: ReviewRating,
    elapsedDays: Double,
): FsrsAlgorithmState {
    val nextDifficulty =
        math.nextDifficulty(
            currentDifficulty = currentMemoryState.difficulty,
            rating = rating,
        )

    // ADR-004 keeps same-day Review re-reviews out of the normal session flow; this branch
    // mirrors py-fsrs `days_since_last_review < 1 → _short_term_stability` so migrations,
    // CLI tooling, and any future policy change use the reference formula for elapsed ≈ 0.
    val nextStability =
        if (elapsedDays < 1.0) {
            math.nextShortTermStability(
                currentStability = currentMemoryState.stability,
                rating = rating,
            )
        } else {
            longTermStability(
                math = math,
                currentMemoryState = currentMemoryState,
                rating = rating,
                elapsedDays = elapsedDays,
            )
        }

    return FsrsAlgorithmState(
        difficulty = nextDifficulty,
        stability = nextStability,
    )
}

private fun longTermStability(
    math: FsrsMath,
    currentMemoryState: FsrsAlgorithmState,
    rating: ReviewRating,
    elapsedDays: Double,
): Double {
    val retrievability =
        math.retrievability(
            elapsedDays = elapsedDays,
            stability = currentMemoryState.stability,
        )

    return when (rating) {
        ReviewRating.Again ->
            math.nextForgetStability(
                difficulty = currentMemoryState.difficulty,
                stability = currentMemoryState.stability,
                retrievability = retrievability,
            )

        ReviewRating.Hard,
        ReviewRating.Good,
        ReviewRating.Easy,
        ->
            math.nextRecallStability(
                difficulty = currentMemoryState.difficulty,
                stability = currentMemoryState.stability,
                retrievability = retrievability,
                rating = rating,
            )
    }
}

private fun scheduledReviewCardData(
    input: InternalSchedulingInput,
    math: FsrsMath,
    nextMemoryState: FsrsAlgorithmState,
): ScheduledCardData =
    when (input.rating) {
        ReviewRating.Again -> {
            val relearningStep = input.parameters.relearningSteps.firstOrNull()

            if (relearningStep == null) {
                ScheduledCardData(
                    state = SrsCardState.Review,
                    stepIndex = null,
                    interval = math.nextIntervalDays(nextMemoryState.stability).days,
                    memoryState = nextMemoryState,
                )
            } else {
                ScheduledCardData(
                    state = SrsCardState.Relearning,
                    stepIndex = 0,
                    interval = relearningStep,
                    memoryState = nextMemoryState,
                )
            }
        }

        ReviewRating.Hard,
        ReviewRating.Good,
        ReviewRating.Easy,
        -> {
            ScheduledCardData(
                state = SrsCardState.Review,
                stepIndex = null,
                interval = math.nextIntervalDays(nextMemoryState.stability).days,
                memoryState = nextMemoryState,
            )
        }
    }
