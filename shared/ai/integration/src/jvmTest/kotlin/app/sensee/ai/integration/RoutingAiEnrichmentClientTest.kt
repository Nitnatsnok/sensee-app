package app.sensee.ai.integration

import app.sensee.ai.core.contract.EnrichmentAvailability
import app.sensee.ai.core.request.DefaultEnrichmentRequestModifiers
import app.sensee.ai.core.request.EnrichmentRequest
import app.sensee.ai.core.request.SenseCoverage
import app.sensee.ai.core.request.UserEnrichmentPreferences
import app.sensee.ai.core.request.UserEnrichmentPreferencesProvider
import app.sensee.ai.curatedEnrichment.CuratedAiEnrichmentClient
import app.sensee.ai.llm.api.LlmHttpClientFactory
import app.sensee.ai.llm.client.LlmAiEnrichmentClientFactory
import app.sensee.ai.llm.config.LlmConfig
import app.sensee.core.observability.analytics.NoOpAnalyticsTracker
import app.sensee.core.observability.crash.NoOpCrashReporter
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.diagnostics.DefaultAppDiagnostics
import app.sensee.core.observability.logging.DefaultAppLoggerFactory
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import app.sensee.settings.domain.AiSettings
import app.sensee.settings.domain.LearningSettings
import app.sensee.settings.domain.LearningTopic
import app.sensee.settings.domain.TopicCatalogRepository
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// jvmTest + runBlocking (not commonTest + runTest): the LLM client factory
// installs Ktor's HttpTimeout plugin, whose real-time delay watcher is
// incompatible with runTest's virtual time — same constraint as
// LlmAiEnrichmentClientTest and SafeBodyTest. The router uses the same
// factory, so the routing exercise has to live here too.
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

    private class FailingSettings : UserSettingsRepository {
        override fun observeSettings(scope: UserSettingsScope): Flow<UserSettingsSnapshot> =
            error("settings unavailable")

        override suspend fun readSettings(scope: UserSettingsScope): UserSettingsSnapshot =
            error("settings unavailable")

        override suspend fun updateSettings(
            scope: UserSettingsScope,
            transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
        ): UserSettingsSnapshot = error("settings unavailable")
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

    // The curated layer is an HTTP source: the client does
    // `GET enrichment/{lemma-slug}`, a miss is a 404. These helpers stand up a
    // Ktor MockEngine serving the curated fixture for known slugs.
    private fun curatedClient(): CuratedAiEnrichmentClient =
        curatedHttpClient { path -> if (path == "enrichment/come-across") COME_ACROSS_RESPONSE else null }

    private fun failingCuratedClient(): CuratedAiEnrichmentClient {
        val engine = MockEngine { respond("upstream boom", HttpStatusCode.InternalServerError) }
        return CuratedAiEnrichmentClient(HttpClient(engine) { expectSuccess = true }, emptySet())
    }

    private fun curatedClientWithItems(
        term: String,
        vararg itemJson: String,
    ): CuratedAiEnrichmentClient {
        val slug = term.trim().lowercase().replace(' ', '-')
        val body = """{"version": 1,"items":[${itemJson.joinToString(",")}]}"""
        return curatedHttpClient { path -> if (path == "enrichment/$slug") body else null }
    }

    private fun curatedHttpClient(respondFor: (path: String) -> String?): CuratedAiEnrichmentClient {
        val engine =
            MockEngine { request ->
                when (val body = respondFor(request.url.encodedPath.trimStart('/'))) {
                    null ->
                        respond(
                            content = """{"error":"not_found"}""",
                            status = HttpStatusCode.NotFound,
                            headers = JSON_HEADERS,
                        )
                    else -> respond(content = body, status = HttpStatusCode.OK, headers = JSON_HEADERS)
                }
            }
        val client =
            HttpClient(engine) {
                expectSuccess = true
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
        return CuratedAiEnrichmentClient(client, emptySet())
    }

    // The LLM client over a test MockEngine; only the engine and configured key
    // vary across cases, the rest of the wiring is constant.
    private fun llmClient(
        engine: MockEngine,
        apiKey: String?,
    ) = LlmAiEnrichmentClientFactory.create(
        httpClient = LlmHttpClientFactory.create(engine = engine, json = Json),
        credentials = { apiKey },
        configProvider = { LlmConfig() },
        taxonomyInvariantsProvider = NoTaxonomyProvider,
        modifiers = DefaultEnrichmentRequestModifiers,
        preferencesProvider = NoOpPreferencesProvider,
        json = Json,
    )

    private fun routerWithFailingLlm(): RoutingAiEnrichmentClient {
        val settings = FakeSettings(UserSettingsSnapshot(ai = AiSettings(aiApiKey = "sk-configured")))
        val failingLlm =
            llmClient(
                MockEngine { respond("upstream boom", HttpStatusCode.InternalServerError) },
                "sk-configured",
            )
        return RoutingAiEnrichmentClient(
            curated = curatedClient(),
            llm = failingLlm,
            settings = settings,
            topicCatalog = FakeTopicCatalog(),
            appDiagnostics = diagnostics(),
        )
    }

    @Test
    fun `a lemma in the curated dataset short-circuits before the LLM is called`() =
        runBlocking {
            val settings = FakeSettings(UserSettingsSnapshot(ai = AiSettings(aiApiKey = "sk-configured")))
            // If the curated layer fails to short-circuit, this LLM mock will
            // execute the error lambda and fail the test - exactly what we want.
            val mustNotBeCalledLlm =
                llmClient(
                    MockEngine { error("LLM must not be called when curated covers the term") },
                    "sk-configured",
                )
            val router =
                RoutingAiEnrichmentClient(
                    curated = curatedClient(),
                    llm = mustNotBeCalledLlm,
                    settings = settings,
                    topicCatalog = FakeTopicCatalog(),
                    appDiagnostics = diagnostics(),
                )

            val result = router.enrich(EnrichmentRequest(term = "come across"))

            assertEquals(EnrichmentAvailability.Available, result.availability)
            assertTrue(
                result.suggestions.size >= 2,
                "the curated 'come across' covers two phrasal-verb variants, got ${result.suggestions.size}",
            )
            // Curated examples carry the new structured shape end-to-end.
            assertTrue(
                result.suggestions.all { suggestion ->
                    suggestion.examples.all { it.sentence.isNotBlank() }
                },
                "every curated example must have a sentence",
            )
            assertTrue(
                result.suggestions.any { suggestion ->
                    suggestion.examples.any { it.translation != null && it.alignment.isNotEmpty() }
                },
                "the curated layer should surface at least one example with translation + alignment",
            )
        }

    @Test
    fun `manual minimal requests bypass curated coverage and use the LLM`() =
        runBlocking {
            val settings = FakeSettings(UserSettingsSnapshot(ai = AiSettings(aiApiKey = "sk-configured")))
            val llm =
                llmClient(
                    MockEngine {
                        respond(
                            content =
                                """{"choices":[{"message":{"role":"assistant","content":${
                                    Json.encodeToString(
                                        """{"version": 1,"items":[{"translation":"ручной смысл"}]}""",
                                    )
                                }}}]}""",
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    },
                    "sk-configured",
                )
            val router =
                RoutingAiEnrichmentClient(
                    curated = curatedClient(),
                    llm = llm,
                    settings = settings,
                    topicCatalog = FakeTopicCatalog(),
                    appDiagnostics = diagnostics(),
                )

            val result =
                router.enrich(
                    EnrichmentRequest(
                        term = "come across",
                        userNote = "значение, которое описал пользователь",
                        senseCoverage = SenseCoverage.Minimal,
                    ),
                )

            assertEquals(listOf("ручной смысл"), result.suggestions.map { it.translation })
        }

    @Test
    fun `topic preferences route a curated-covered lemma to the LLM`() =
        runBlocking {
            val settings =
                FakeSettings(
                    UserSettingsSnapshot(
                        ai = AiSettings(aiApiKey = "sk-configured"),
                        learning = LearningSettings(preferredTopicIds = setOf("business")),
                    ),
                )
            val llm =
                llmClient(
                    MockEngine {
                        respond(
                            content =
                                """{"choices":[{"message":{"role":"assistant","content":${
                                    Json.encodeToString(
                                        """{"version": 1,"items":[{"translation":"topic-steered смысл"}]}""",
                                    )
                                }}}]}""",
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    },
                    "sk-configured",
                )
            val router =
                RoutingAiEnrichmentClient(
                    curated = curatedClient(),
                    llm = llm,
                    settings = settings,
                    topicCatalog =
                        FakeTopicCatalog(
                            listOf(
                                LearningTopic(id = "business", displayName = "Business", promptKeyword = "business"),
                            ),
                        ),
                    appDiagnostics = diagnostics(),
                )

            val result = router.enrich(EnrichmentRequest(term = "come across"))

            // Curated covers "come across", but topic steering routes past it: the
            // answer is the LLM's, not the curated variants.
            assertEquals(listOf("topic-steered смысл"), result.suggestions.map { it.translation })
        }

    @Test
    fun `saved topic preference ids route past curated even when no keyword resolves`() =
        runBlocking {
            val settings =
                FakeSettings(
                    UserSettingsSnapshot(
                        ai = AiSettings(aiApiKey = "sk-configured"),
                        learning = LearningSettings(preferredTopicIds = setOf("missing")),
                    ),
                )
            val catalog = FakeTopicCatalog(topics = emptyList())
            val llm =
                llmClient(
                    MockEngine {
                        respond(
                            content =
                                """{"choices":[{"message":{"role":"assistant","content":${
                                    Json.encodeToString(
                                        """{"version": 1,"items":[{"translation":"LLM без темы"}]}""",
                                    )
                                }}}]}""",
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    },
                    "sk-configured",
                )
            val router =
                RoutingAiEnrichmentClient(
                    curated = curatedClient(),
                    llm = llm,
                    settings = settings,
                    topicCatalog = catalog,
                    appDiagnostics = diagnostics(),
                )

            val result = router.enrich(EnrichmentRequest(term = "come across"))

            assertEquals(listOf("LLM без темы"), result.suggestions.map { it.translation })
            assertEquals(1, catalog.calls)
        }

    @Test
    fun `degraded curated suggestions do not bypass a configured LLM`() =
        runBlocking {
            val settings = FakeSettings(UserSettingsSnapshot(ai = AiSettings(aiApiKey = "sk-configured")))
            var llmCalls = 0
            val llm =
                llmClient(
                    MockEngine {
                        llmCalls++
                        respond(
                            content =
                                """{"choices":[{"message":{"role":"assistant","content":${
                                    Json.encodeToString(
                                        """{"version": 1,"items":[{"translation":"богатый LLM-вариант"},{"translation":"второй LLM-вариант"}]}""",
                                    )
                                }}}]}""",
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    },
                    "sk-configured",
                )
            val router =
                RoutingAiEnrichmentClient(
                    curated =
                        curatedClientWithItems(
                            term = "rough",
                            """{"translation":"черновой curated-вариант","surface_form":"rough","unit_type":"verb"}""",
                            // Structurally invalid item (component without a `role`) → dropped as malformed.
                            """{"components":[{"text":"x"}]}""",
                        ),
                    llm = llm,
                    settings = settings,
                    topicCatalog = FakeTopicCatalog(),
                    appDiagnostics = diagnostics(),
                )

            val result = router.enrich(EnrichmentRequest(term = "rough"))

            assertEquals(1, llmCalls)
            assertEquals(EnrichmentAvailability.Available, result.availability)
            assertEquals(
                listOf("богатый LLM-вариант", "второй LLM-вариант"),
                result.suggestions.map { it.translation },
            )
        }

    @Test
    fun `a lemma not in the curated dataset and no key configured reports Unavailable`() =
        runBlocking {
            val settings = FakeSettings(UserSettingsSnapshot(ai = AiSettings(aiApiKey = null)))
            val guardedLlm =
                llmClient(
                    MockEngine { error("LLM must not be called when no key is configured") },
                    null,
                )
            val router =
                RoutingAiEnrichmentClient(
                    curated = curatedClient(),
                    llm = guardedLlm,
                    settings = settings,
                    topicCatalog = FakeTopicCatalog(),
                    appDiagnostics = diagnostics(),
                )

            val result = router.enrich(EnrichmentRequest(term = "run"))

            assertTrue(
                result.availability is EnrichmentAvailability.Unavailable,
                "no-key + curated-miss must surface Unavailable, not Available; got ${result.availability}",
            )
            assertEquals(emptyList(), result.suggestions)
        }

    @Test
    fun `curated failure with no configured key reports Degraded rather than Unavailable`() =
        runBlocking {
            val settings = FakeSettings(UserSettingsSnapshot(ai = AiSettings(aiApiKey = null)))
            val guardedLlm =
                llmClient(
                    MockEngine { error("LLM must not be called when no key is configured") },
                    null,
                )
            val router =
                RoutingAiEnrichmentClient(
                    curated = failingCuratedClient(),
                    llm = guardedLlm,
                    settings = settings,
                    topicCatalog = FakeTopicCatalog(),
                    appDiagnostics = diagnostics(),
                )

            val result = router.enrich(EnrichmentRequest(term = "come across"))

            assertTrue(result.availability is EnrichmentAvailability.Degraded)
            assertEquals(emptyList(), result.suggestions)
        }

    @Test
    fun `a blank key is treated as unconfigured and also reports Unavailable on a curated miss`() =
        runBlocking {
            val settings = FakeSettings(UserSettingsSnapshot(ai = AiSettings(aiApiKey = "   ")))
            val guardedLlm =
                llmClient(
                    MockEngine { error("LLM must not be called when no key is configured") },
                    null,
                )
            val router =
                RoutingAiEnrichmentClient(
                    curated = curatedClient(),
                    llm = guardedLlm,
                    settings = settings,
                    topicCatalog = FakeTopicCatalog(),
                    appDiagnostics = diagnostics(),
                )

            val result = router.enrich(EnrichmentRequest(term = "walk"))

            assertTrue(result.availability is EnrichmentAvailability.Unavailable)
            assertEquals(emptyList(), result.suggestions)
        }

    @Test
    fun `a settings read failure fails closed and does not call the LLM`() =
        runBlocking {
            val guardedLlm =
                llmClient(
                    MockEngine { error("LLM must not be called when settings cannot prove a key exists") },
                    null,
                )
            val router =
                RoutingAiEnrichmentClient(
                    curated = curatedClient(),
                    llm = guardedLlm,
                    settings = FailingSettings(),
                    topicCatalog = FakeTopicCatalog(),
                    appDiagnostics = diagnostics(),
                )

            val result = router.enrich(EnrichmentRequest(term = "run"))

            assertTrue(result.availability is EnrichmentAvailability.Unavailable)
            assertEquals(emptyList(), result.suggestions)
        }

    @Test
    fun `a configured key whose LLM fails degrades on a curated miss`() =
        runBlocking {
            val result = routerWithFailingLlm().enrich(EnrichmentRequest(term = "run"))

            assertTrue(
                result.availability != EnrichmentAvailability.Available,
                "a failing LLM degrades the seam, it does not report Available",
            )
            assertEquals(emptyList(), result.suggestions)
        }

    @Test
    fun `no configured key does not load topic catalog even when topic preferences are saved`() =
        runBlocking {
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
                llmClient(
                    MockEngine { error("LLM must not be called when no key is configured") },
                    null,
                )
            val router =
                RoutingAiEnrichmentClient(
                    curated = curatedClient(),
                    llm = guardedLlm,
                    settings = settings,
                    topicCatalog = catalog,
                    appDiagnostics = diagnostics(),
                )

            val result = router.enrich(EnrichmentRequest(term = "run"))

            assertTrue(result.availability is EnrichmentAvailability.Unavailable)
            assertEquals(0, catalog.calls)
        }

    @Test
    fun `preferred topic ids are resolved through the catalog into the enrichment request`() =
        runBlocking {
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
                llmClient(
                    MockEngine { httpRequest ->
                        promptBodies += httpRequest.body.toByteArray().decodeToString()
                        respond(
                            content =
                                """{"choices":[{"message":{"role":"assistant","content":${
                                    Json.encodeToString(
                                        """{"version": 1,"items":[{"translation":"x"},{"translation":"y"}]}""",
                                    )
                                }}}]}""",
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    },
                    "sk-configured",
                )
            val router =
                RoutingAiEnrichmentClient(
                    curated = curatedClient(),
                    llm = llm,
                    settings = settings,
                    topicCatalog = catalog,
                    appDiagnostics = diagnostics(),
                )

            router.enrich(EnrichmentRequest(term = "come across"))

            val promptBody = promptBodies.joinToString("\n")
            assertTrue(promptBody.contains("travel and tourism"), "selected topic keyword reaches the prompt")
            assertTrue(promptBody.contains("food and cooking"), "all selected topic keywords reach the prompt")
            assertTrue(!promptBody.contains("sports and fitness"), "an unselected topic does not")
        }
}

private val NoTaxonomyProvider: TaxonomyInvariantsProvider = TaxonomyInvariantsProvider { null }

private val NoOpPreferencesProvider: UserEnrichmentPreferencesProvider =
    UserEnrichmentPreferencesProvider { UserEnrichmentPreferences.EMPTY }

private val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

// Stand-in for the `enrichment/come-across` fixture: two phrasal-verb variants,
// each with an example carrying a translation + alignment.
private val COME_ACROSS_RESPONSE =
    """
    {
      "version": 1,
      "items": [
        {
          "translation": "наткнуться",
          "surface_form": "come across <something>",
          "unit_type": "phrasal_verb",
          "examples": [
            {
              "sentence": "I came across an old letter.",
              "translation": "Я наткнулся на старое письмо.",
              "alignment": [ { "source": "came across", "target": "наткнулся на" } ]
            }
          ],
          "cefr": "B2"
        },
        {
          "translation": "производить впечатление",
          "surface_form": "come across [as]",
          "unit_type": "phrasal_verb",
          "examples": [
            {
              "sentence": "She comes across as curious.",
              "translation": "Она производит впечатление любопытной.",
              "alignment": [ { "source": "comes across as", "target": "производит впечатление" } ]
            }
          ]
        }
      ]
    }
    """.trimIndent()
