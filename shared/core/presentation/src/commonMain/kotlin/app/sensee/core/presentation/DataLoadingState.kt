package app.sensee.core.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch

/**
 * Data-loading state for the UI layer.
 */
public sealed interface DataLoadingState {
    public data object Idle : DataLoadingState

    public data object Success : DataLoadingState

    public data object Loading : DataLoadingState

    public data class Error(
        val throwable: Throwable,
    ) : DataLoadingState
}

public fun <T> Result<T>.toDataLoadingState(): DataLoadingState =
    this.fold(
        onSuccess = { DataLoadingState.Success },
        onFailure = { DataLoadingState.Error(it) },
    )

public fun DataLoadingState.isError(): Boolean = this is DataLoadingState.Error

public fun DataLoadingState.isIdle(): Boolean = this == DataLoadingState.Idle

public fun DataLoadingState.isLoading(): Boolean = this == DataLoadingState.Loading

public fun DataLoadingState.isSuccess(): Boolean = this == DataLoadingState.Success

/**
 * Aggregates several loading states into a single resulting state.
 *
 * Priority order:
 * - [DataLoadingState.Error] wins over everything.
 * - [DataLoadingState.Loading] beats [DataLoadingState.Idle]: at least one request is already
 *   running, so the UI should show loading even if another part has not started yet.
 * - [DataLoadingState.Idle] beats [DataLoadingState.Success]: the UI may render them differently.
 * - With no states, returns [DataLoadingState.Idle].
 */
public fun merge(vararg states: DataLoadingState): DataLoadingState =
    states.ifEmpty { arrayOf(DataLoadingState.Idle) }.reduce { acc, state ->
        when {
            acc.isError() -> acc
            state.isError() -> state
            acc.isLoading() -> acc
            state.isLoading() -> state
            acc == DataLoadingState.Idle -> acc
            state == DataLoadingState.Idle -> state
            else -> acc
        }
    }

public fun DataLoadingState.merge(state: DataLoadingState): DataLoadingState = merge(this, state)

/**
 * Like [catch], but also emits [DataLoadingState.Error] to [collector].
 *
 * @param collector receives the loading state on failure.
 */
public fun <T> Flow<T>.catchToLoadingState(collector: FlowCollector<DataLoadingState>): Flow<T> =
    catch {
        val state = DataLoadingState.Error(it)
        collector.emit(state)
    }
