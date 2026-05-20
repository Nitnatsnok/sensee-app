package app.sensee.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

public actual fun createRealHttpClientEngine(): HttpClientEngine = CIO.create()
