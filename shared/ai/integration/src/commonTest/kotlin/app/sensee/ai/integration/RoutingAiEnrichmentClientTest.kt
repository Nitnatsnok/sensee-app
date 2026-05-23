package app.sensee.ai.integration

import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import app.sensee.ai.fixture.FixtureAiEnrichmentClient
import app.sensee.ai.llm.client.LlmAiEnrichmentClientFactory
import app.sensee.ai.llm.config.LlmConfig
import app.sensee.core.observability.analytics.NoOpAnalyticsTracker
import app.sensee.core.observability.crash.NoOpCrashReporter
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.diagnostics.DefaultAppDiagnostics
import app.sensee.core.observability.logging.DefaultAppLoggerFactory
import app.sensee.settings.domain.AiSettings
import app.sensee.settings.domain.LearningSettings
import app.sensee.settings.domain.LearningTopic
import app.sensee.settings.domain.TopicCatalogRepository
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
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

    private class FakeTopicCatalog(
        private val topics: List<LearningTopic> = emptyList(),
    ) : TopicCatalogRepository {
        var calls: Int = 0
            private set

        override suspend fun topics(): List<LearningTopic> {
            calls++
            return topics
        }
    }

    private fun diagnostics(): AppDiagnostics =
        DefaultAppDiagnostics(
            logger = DefaultAppLoggerFactory().tagged("Test"),
            crashReporter = NoOpCrashReporter,
            analyticsTracker = NoOpAnalyticsTracker,
        )

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
            topicCatalog = FakeTopicCatalog(),
            appDiagnostics = diagnostics(),
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
            topicCatalog = FakeTopicCatalog(),
            appDiagnostics = diagnostics(),
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

    @Test
    fun `no configured key does not load topic catalog even when topic preferences are saved`() =
        runTest {
            val catalog =
                FakeTopicCatalog(
                    listOf(LearningTopic("travel", "Путешествия", "travel and tourism")),
                )
            val settings =
                FakeSettings(
                    UserSettingsSnapshot(
                        learning = LearningSettings(preferredTopicIds = setOf("travel")),
                        ai = AiSettings(aiApiKey = null),
                    ),
                )
            val guardedLlm =
                LlmAiEnrichmentClientFactory.create(
                    engine = MockEngine { error("LLM must not be called when no key is configured") },
                    credentials = { null },
                    configProvider = { LlmConfig() },
                    json = Json,
                )
            val router =
                RoutingAiEnrichmentClient(
                    fixture = FixtureAiEnrichmentClient(),
                    llm = guardedLlm,
                    settings = settings,
                    topicCatalog = catalog,
                    appDiagnostics = diagnostics(),
                )

            val result = router.enrich(EnrichmentRequest(term = "run"))

            assertEquals(EnrichmentAvailability.Available, result.availability)
            assertTrue(result.suggestions.isNotEmpty())
            assertEquals(0, catalog.calls)
        }

    @Test
    fun `preferred topic ids are resolved through the catalog into the enrichment request`() =
        runTest {
            val promptBodies = mutableListOf<String>()
            val settings =
                FakeSettings(
                    UserSettingsSnapshot(
                        learning = LearningSettings(preferredTopicIds = setOf("travel", "food")),
                        ai = AiSettings(aiApiKey = "sk-configured"),
                    ),
                )
            val catalog =
                FakeTopicCatalog(
                    listOf(
                        LearningTopic("travel", "Путешествия", "travel and tourism"),
                        LearningTopic("food", "Еда", "food and cooking"),
                        LearningTopic("sports", "Спорт", "sports and fitness"),
                    ),
                )
            val llm =
                LlmAiEnrichmentClientFactory.create(
                    engine =
                        MockEngine { httpRequest ->
                            promptBodies += httpRequest.body.toByteArray().decodeToString()
                            respond(
                                content =
                                    """{"choices":[{"message":{"role":"assistant","content":${
                                        Json.encodeToString(
                                            """{"version":1,"items":[{"translation":"x"},{"translation":"y"}]}""",
                                        )
                                    }}}]}""",
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                            )
                        },
                    credentials = { "sk-configured" },
                    configProvider = { LlmConfig() },
                    json = Json,
                )
            val router =
                RoutingAiEnrichmentClient(
                    fixture = FixtureAiEnrichmentClient(),
                    llm = llm,
                    settings = settings,
                    topicCatalog = catalog,
                    appDiagnostics = diagnostics(),
                )

            router.enrich(EnrichmentRequest(term = "run"))

            val promptBody = promptBodies.joinToString("\n")
            assertTrue(promptBody.contains("travel and tourism"), "selected topic keyword reaches the prompt")
            assertTrue(promptBody.contains("food and cooking"), "all selected topic keywords reach the prompt")
            assertTrue(!promptBody.contains("sports and fitness"), "an unselected topic does not")
        }
}
