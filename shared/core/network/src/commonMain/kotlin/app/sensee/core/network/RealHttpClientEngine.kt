package app.sensee.core.network

import io.ktor.client.engine.HttpClientEngine

/**
 * A real, network-hitting engine per platform (CIO/Android/Darwin/Js). The
 * default DI [HttpClientEngine] is the mock backend; live integrations
 * (LLM, ElevenLabs) need an actual transport and resolve it through this
 * factory instead, keeping the mock/real split explicit and deliberate.
 */
public expect fun createRealHttpClientEngine(): HttpClientEngine
