package app.sensee.core.network

public data class NetworkConfig(
    val baseUrl: String,
    val requestTimeoutMillis: Long = DEFAULT_REQUEST_TIMEOUT_MILLIS,
    val connectTimeoutMillis: Long = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    val socketTimeoutMillis: Long = DEFAULT_SOCKET_TIMEOUT_MILLIS,
) {
    public companion object {
        public const val DEFAULT_REQUEST_TIMEOUT_MILLIS: Long = 30_000L
        public const val DEFAULT_CONNECT_TIMEOUT_MILLIS: Long = 15_000L
        public const val DEFAULT_SOCKET_TIMEOUT_MILLIS: Long = 30_000L
    }
}
