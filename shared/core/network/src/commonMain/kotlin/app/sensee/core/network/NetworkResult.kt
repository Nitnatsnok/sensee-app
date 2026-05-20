package app.sensee.core.network

public sealed interface NetworkResult<out T> {
    public data class Success<T>(
        val value: T,
    ) : NetworkResult<T>

    public data class Failure(
        val error: NetworkError,
    ) : NetworkResult<Nothing>
}

public fun <T> NetworkResult<T>.getOrThrowNetworkException(): T =
    when (this) {
        is NetworkResult.Success -> value
        is NetworkResult.Failure -> throw NetworkException(error)
    }
