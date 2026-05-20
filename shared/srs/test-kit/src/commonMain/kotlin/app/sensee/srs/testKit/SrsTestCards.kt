package app.sensee.srs.testKit

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.id.SrsParametersId
import app.sensee.srs.core.model.SrsAlgorithmInfo
import app.sensee.srs.core.model.SrsAlgorithmState
import app.sensee.srs.core.model.SrsCardSnapshot
import app.sensee.srs.core.model.SrsCardState
import app.sensee.srs.engine.factory.SrsCardFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

public object SrsTestCards {
    public fun newCard(id: String = "card-1"): SrsCardSnapshot =
        SrsCardFactory.newCard(
            id = SrsCardId(id),
        )

    public fun dueReviewCard(
        id: String = "card-1",
        dueAt: Instant = SrsTestInstants.Base,
        lastReviewedAt: Instant = SrsTestInstants.Base,
        scheduledInterval: Duration = 1.days,
        reviewCount: Int = 1,
        lapseCount: Int = 0,
        algorithmState: SrsAlgorithmState? = null,
        algorithm: SrsAlgorithmInfo? = null,
        parametersId: SrsParametersId? = null,
    ): SrsCardSnapshot =
        SrsCardSnapshot(
            id = SrsCardId(id),
            state = SrsCardState.Review,
            dueAt = dueAt,
            lastReviewedAt = lastReviewedAt,
            scheduledInterval = scheduledInterval,
            reviewCount = reviewCount,
            lapseCount = lapseCount,
            stepIndex = null,
            algorithmState = algorithmState,
            algorithm = algorithm,
            parametersId = parametersId,
        )

    public fun learningCard(
        id: String = "card-1",
        scheduledInterval: Duration = 1.minutes,
        state: SrsCardState = SrsCardState.Learning,
        stepIndex: Int = 0,
        reviewCount: Int = 1,
        lapseCount: Int = 0,
    ): SrsCardSnapshot =
        SrsCardSnapshot(
            id = SrsCardId(id),
            state = state,
            dueAt = SrsTestInstants.Base,
            lastReviewedAt = SrsTestInstants.Base,
            scheduledInterval = scheduledInterval,
            reviewCount = reviewCount,
            lapseCount = lapseCount,
            stepIndex = stepIndex,
            algorithmState = null,
            algorithm = null,
            parametersId = null,
        )

    public fun suspendedCard(id: String = "card-1"): SrsCardSnapshot =
        newCard(id).copy(
            state = SrsCardState.Suspended,
        )
}
