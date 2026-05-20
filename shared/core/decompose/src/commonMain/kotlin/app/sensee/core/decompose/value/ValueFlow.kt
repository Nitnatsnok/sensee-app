package app.sensee.core.decompose.value

import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Bridges a decompose [Value] (the navigation/router state handle) into a cold [Flow].
 *
 * Decompose emits the current value synchronously on subscription, so collectors get an
 * immediate first element. This is the seam that lets a parent derive chrome (bottom bar,
 * etc.) from its own router state declaratively — `stack.asFlow().flatMapLatest { ... }` —
 * instead of hand-managing subscribe/cancel jobs.
 */
public fun <T : Any> Value<T>.asFlow(): Flow<T> =
    callbackFlow {
        val cancellation = subscribe { trySend(it) }
        awaitClose { cancellation.cancel() }
    }
