package app.sensee.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

public actual fun createRealHttpClientEngine(): HttpClientEngine = Js.create()
