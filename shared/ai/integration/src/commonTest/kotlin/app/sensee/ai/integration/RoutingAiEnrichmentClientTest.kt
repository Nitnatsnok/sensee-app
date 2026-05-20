package app.sensee.ai.integration

import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.ai.fixture.FixtureAiEnrichmentClient
import app.sensee.ai.llm.client.LlmAiEnrichmentClientFactory
import app.sensee.ai.llm.config.LlmConfig
import app.sensee.core.observability.analytics.NoOpAnalyticsTracker
import app.sensee.core.observability.crash.NoOpCrashReporter
import app.sensee.core.observability.diagnostics.DefaultAppDiagnostics
import app.sensee.core.observability.logging.DefaultAppLoggerFactory
import app.sensee.settings.domain.AiSettings
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutingAiEnrichmentClientTest {
    private class FakeSettings(
        private val snapshot: UserSettingsSnapshot,
    ) : UserSettingsRepository {
        override fun observeSettings(scope: UserSettingsScope): Flow<UserSettingsSnapshot> = flowOf(snapshot)

        override suspend fun readSettings(scope: UserSettingsScope): UserSettingsSnapshot = snapshot

        override suspend fun updateSettings(
            scope: UserSettingsScope,
            transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
        ): UserSettingsSnapshot = transform(snapshot)
    }

    private fun router(ai: AiSettings): RoutingAiEnrichmentClient {
        val settings = FakeSettings(UserSettingsSnapshot(ai = ai))
        val guardedLlm =
            LlmAiEnrichmentClientFactory.create(
                engine = MockEngine { error("LLM must not be called when no key is configured") },
                credentials = { null },
                configProvider = { LlmConfig() },
                json = Json,
            )
        return RoutingAiEnrichmentClient(
            fixture = FixtureAiEnrichmentClient(),
            llm = guardedLlm,
            settings = settings,
            appDiagnostics =
                DefaultAppDiagnostics(
                    logger = DefaultAppLoggerFactory().tagged("Test"),
                    crashReporter = NoOpCrashReporter,
                    analyticsTracker = NoOpAnalyticsTracker,
                ),
        )
    }

    private fun routerWithFailingLlm(): RoutingAiEnrichmentClient {
        val settings = FakeSettings(UserSettingsSnapshot(ai = AiSettings(aiApiKey = "sk-configured")))
        val failingLlm =
            LlmAiEnrichmentClientFactory.create(
                engine = MockEngine { respond("upstream boom", HttpStatusCode.InternalServerError) },
                credentials = { "sk-configured" },
                configProvider = { LlmConfig() },
                json = Json,
            )
        return RoutingAiEnrichmentClient(
            fixture = FixtureAiEnrichmentClient(),
            llm = failingLlm,
            settings = settings,
            appDiagnostics =
                DefaultAppDiagnostics(
                    logger = DefaultAppLoggerFactory().tagged("Test"),
                    crashReporter = NoOpCrashReporter,
                    analyticsTracker = NoOpAnalyticsTracker,
                ),
        )
    }

    @Test
    fun `a configured key whose LLM fails degrades rather than silently falling back to the fixture`() =
        runTest {
            val result = routerWithFailingLlm().enrich(EnrichmentRequest(term = "run"))

            assertTrue(
                result.availability != EnrichmentAvailability.Available,
                "a failing LLM degrades the seam, it does not report Available",
            )
            assertTrue(
                result.suggestions.isEmpty(),
                "a failing LLM must not be masked by fixture suggestions (ADR-005)",
            )
        }

    @Test
    fun `no configured key routes to the offline fixture`() =
        runTest {
            val result =
                router(AiSettings(aiApiKey = null)).enrich(
                    EnrichmentRequest(term = "run"),
                )

            assertEquals(EnrichmentAvailability.Available, result.availability)
            assertTrue(result.suggestions.isNotEmpty())
        }

    @Test
    fun `blank key is treated as unconfigured and still routes to fixture`() =
        runTest {
            val result =
                router(AiSettings(aiApiKey = "   ")).enrich(
                    EnrichmentRequest(term = "walk"),
                )

            assertEquals(EnrichmentAvailability.Available, result.availability)
            assertTrue(result.suggestions.isNotEmpty())
        }
}
