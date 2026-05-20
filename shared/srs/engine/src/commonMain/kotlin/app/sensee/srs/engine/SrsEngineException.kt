package app.sensee.srs.engine

import app.sensee.srs.core.id.SrsCardId

public sealed class SrsEngineException(
    message: String,
) : IllegalStateException(message)

public class SrsCardNotFoundException(
    public val cardId: SrsCardId,
) : SrsEngineException(
        message = "SRS card not found: $cardId",
    )
