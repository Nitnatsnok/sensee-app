package app.sensee.core.coroutines

import kotlinx.coroutines.CancellationException

/**
 * Like [runCatching] but cooperative with structured concurrency: a
 * [CancellationException] is rethrown so coroutine cancellation unwinds normally,
 * instead of being captured into a spurious error [Result].
 */
public inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }
