package app.sensee.srs.engine.id

import app.sensee.srs.core.id.SrsReviewLogId

public fun interface SrsReviewLogIdGenerator {
    public fun nextId(): SrsReviewLogId
}
