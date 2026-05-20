package app.sensee.feature.practice.data.local

import app.sensee.core.database.Practice_srs_card
import app.sensee.core.database.SelectDuePracticeSrsCards
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.id.SrsParametersId
import app.sensee.srs.core.model.SrsAlgorithmInfo
import app.sensee.srs.core.model.SrsAlgorithmState
import app.sensee.srs.core.model.SrsCardSnapshot
import app.sensee.srs.core.model.SrsCardState
import app.sensee.srs.fsrs.FsrsAlgorithmState
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

internal fun Practice_srs_card.toSrsCardSnapshot(): SrsCardSnapshot {
    val algorithm = buildAlgorithmInfo(algorithm_name, algorithm_version)
    return SrsCardSnapshot(
        id = SrsCardId(card_id),
        state = SrsCardState.valueOf(state),
        dueAt = due_at_epoch_ms?.let(Instant::fromEpochMilliseconds),
        lastReviewedAt = last_reviewed_at_epoch_ms?.let(Instant::fromEpochMilliseconds),
        scheduledInterval = scheduled_interval_ms?.milliseconds,
        reviewCount = review_count.toInt(),
        lapseCount = lapse_count.toInt(),
        stepIndex = step_index?.toInt(),
        algorithmState = buildAlgorithmState(algorithm, fsrs_difficulty, fsrs_stability),
        algorithm = algorithm,
        parametersId = parameters_id?.let(::SrsParametersId),
    )
}

internal fun SelectDuePracticeSrsCards.toSrsCardSnapshot(): SrsCardSnapshot {
    val algorithm = buildAlgorithmInfo(algorithm_name, algorithm_version)
    return SrsCardSnapshot(
        id = SrsCardId(card_id),
        state = SrsCardState.valueOf(state),
        dueAt = Instant.fromEpochMilliseconds(due_at_epoch_ms),
        lastReviewedAt = last_reviewed_at_epoch_ms?.let(Instant::fromEpochMilliseconds),
        scheduledInterval = scheduled_interval_ms?.milliseconds,
        reviewCount = review_count.toInt(),
        lapseCount = lapse_count.toInt(),
        stepIndex = step_index?.toInt(),
        algorithmState = buildAlgorithmState(algorithm, fsrs_difficulty, fsrs_stability),
        algorithm = algorithm,
        parametersId = parameters_id?.let(::SrsParametersId),
    )
}

private fun buildAlgorithmInfo(
    name: String?,
    version: String?,
): SrsAlgorithmInfo? {
    if (name == null || version == null) return null
    return SrsAlgorithmInfo(name = name, version = version)
}

private fun buildAlgorithmState(
    algorithm: SrsAlgorithmInfo?,
    difficulty: Double?,
    stability: Double?,
): SrsAlgorithmState? {
    if (algorithm == null || difficulty == null || stability == null) return null
    return FsrsAlgorithmState(algorithm = algorithm, difficulty = difficulty, stability = stability)
}
