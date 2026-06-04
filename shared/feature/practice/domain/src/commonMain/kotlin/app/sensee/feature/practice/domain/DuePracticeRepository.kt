package app.sensee.feature.practice.domain

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * Narrow due-count seam for dashboards (e.g. Home) over SRS state keyed by sense.
 * A consumer depends on this instead of Library, so surfacing a due count never
 * transitively pulls in the catalog projection. [now] is the moment "due" is
 * measured against; the observed flow re-emits as reviews change that state.
 */
public interface DuePracticeRepository {
    public suspend fun countDue(now: Instant): Int

    public fun observeDueCount(now: Instant): Flow<Int>
}
