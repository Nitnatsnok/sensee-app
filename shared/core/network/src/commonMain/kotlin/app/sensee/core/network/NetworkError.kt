package app.sensee.core.network

public sealed interface NetworkError {
    public data class Http(
        val statusCode: Int,
        val responseBody: String?,
        val message: String?,
    ) : NetworkError

    public data class Timeout(
        val message: String?,
    ) : NetworkError

    public data class Connectivity(
        val message: String?,
    ) : NetworkError

    public data class Serialization(
        val message: String?,
    ) : NetworkError

    public data class Unknown(
        val message: String?,
    ) : NetworkError
}
