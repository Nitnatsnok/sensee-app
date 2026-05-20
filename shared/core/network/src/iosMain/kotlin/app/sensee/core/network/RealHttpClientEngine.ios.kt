package app.sensee.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

public actual fun createRealHttpClientEngine(): HttpClientEngine = Darwin.create()
