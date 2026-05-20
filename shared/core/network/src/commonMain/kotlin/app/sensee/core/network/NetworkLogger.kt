package app.sensee.core.network

import io.ktor.client.plugins.logging.Logger

public fun interface NetworkLogger {
    public fun log(message: String)
}

public object NoOpNetworkLogger : NetworkLogger {
    override fun log(message: String): Unit = Unit
}

internal class KtorNetworkLogger(
    private val delegate: NetworkLogger,
) : Logger {
    override fun log(message: String) {
        delegate.log(message)
    }
}
