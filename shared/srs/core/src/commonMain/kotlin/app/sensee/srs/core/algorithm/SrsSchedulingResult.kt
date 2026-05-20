package app.sensee.srs.core.algorithm

import app.sensee.srs.core.log.SrsReviewLog
import app.sensee.srs.core.model.SrsCardSnapshot

public data class SrsSchedulingResult(
    val updatedCard: SrsCardSnapshot,
    val reviewLog: SrsReviewLog,
)
