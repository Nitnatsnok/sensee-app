package app.sensee.srs.engine.storage

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.log.SrsReviewLog

public interface SrsReviewLogStore {
    public suspend fun appendReviewLog(log: SrsReviewLog)

    public suspend fun getReviewLogs(
        cardId: SrsCardId,
        limit: Int? = null,
    ): List<SrsReviewLog>
}
