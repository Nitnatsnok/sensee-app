package app.sensee.srs.core.algorithm

import app.sensee.srs.core.model.SrsAlgorithmParameters
import app.sensee.srs.core.model.SrsCardSnapshot
import kotlin.time.Instant

public data class SrsSchedulingPreviewInput<Parameters : SrsAlgorithmParameters>(
    val card: SrsCardSnapshot,
    val reviewedAt: Instant,
    val parameters: Parameters,
)
