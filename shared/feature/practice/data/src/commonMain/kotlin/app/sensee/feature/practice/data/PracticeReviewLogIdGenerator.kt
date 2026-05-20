package app.sensee.feature.practice.data

import app.sensee.srs.core.id.SrsReviewLogId
import app.sensee.srs.engine.id.SrsReviewLogIdGenerator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.time.Clock

@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<SrsReviewLogIdGenerator>(),
)
@Inject
public class PracticeReviewLogIdGenerator(
    private val clock: Clock,
) : SrsReviewLogIdGenerator {
    private var nextValue = 1

    override fun nextId(): SrsReviewLogId {
        val value = nextValue
        nextValue += 1
        return SrsReviewLogId("practice-review-${clock.now().toEpochMilliseconds()}-$value")
    }
}
