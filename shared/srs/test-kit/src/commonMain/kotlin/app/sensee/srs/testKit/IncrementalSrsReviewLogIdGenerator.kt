package app.sensee.srs.testKit

import app.sensee.srs.core.id.SrsReviewLogId
import app.sensee.srs.engine.id.SrsReviewLogIdGenerator

public class IncrementalSrsReviewLogIdGenerator(
    private val prefix: String = "review-log",
    startFrom: Int = 1,
) : SrsReviewLogIdGenerator {
    private var nextValue: Int = startFrom

    override fun nextId(): SrsReviewLogId {
        val id = SrsReviewLogId("$prefix-$nextValue")
        nextValue += 1
        return id
    }
}
