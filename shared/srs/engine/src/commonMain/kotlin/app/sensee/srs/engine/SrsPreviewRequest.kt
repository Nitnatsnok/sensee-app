package app.sensee.srs.engine

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.id.SrsScope
import kotlin.time.Instant

public data class SrsPreviewRequest(
    val cardId: SrsCardId,
    val reviewedAt: Instant? = null,
    val scope: SrsScope = SrsScope.Default,
)
