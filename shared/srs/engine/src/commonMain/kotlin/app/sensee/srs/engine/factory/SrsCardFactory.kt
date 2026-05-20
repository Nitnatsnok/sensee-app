package app.sensee.srs.engine.factory

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.model.SrsCardSnapshot
import app.sensee.srs.core.model.SrsCardState

public object SrsCardFactory {
    public fun newCard(id: SrsCardId): SrsCardSnapshot =
        SrsCardSnapshot(
            id = id,
            state = SrsCardState.New,
            dueAt = null,
            lastReviewedAt = null,
            scheduledInterval = null,
            reviewCount = 0,
            lapseCount = 0,
            stepIndex = null,
            algorithmState = null,
            algorithm = null,
            parametersId = null,
        )
}
