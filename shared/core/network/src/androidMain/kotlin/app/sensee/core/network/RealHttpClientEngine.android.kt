package app.sensee.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android

public actual fun createRealHttpClientEngine(): HttpClientEngine = Android.create()
