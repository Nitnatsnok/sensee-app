package app.sensee.core.network

public class NetworkException(
    public val error: NetworkError,
    cause: Throwable? = null,
) : Exception(error.toString(), cause)
