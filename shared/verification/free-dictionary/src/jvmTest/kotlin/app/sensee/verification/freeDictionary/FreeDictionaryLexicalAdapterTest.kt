package app.sensee.verification.freeDictionary

import app.sensee.verification.core.LexicalExistence
import app.sensee.verification.core.LexicalVerificationQuery
import app.sensee.verification.core.PartOfSpeechHint
import app.sensee.verification.core.VerificationPolicy
import app.sensee.verification.core.VerifierAvailability
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FreeDictionaryLexicalAdapterTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a confirmed entry surfaces POS and IPA from the success payload`() {
        val engine =
            MockEngine { request ->
                assertTrue(request.url.encodedPath.endsWith("/hello"))
                respond(
                    content = HELLO_SUCCESS_JSON,
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            }
        val adapter = adapter(engine)

        val result = runBlocking { adapter.lookup(query("hello")) }

        assertEquals(VerifierAvailability.Available, result.availability)
        assertEquals(LexicalExistence.Confirmed, result.existence)
        assertEquals(listOf(PartOfSpeechHint("noun"), PartOfSpeechHint("verb")), result.partsOfSpeech)
        // Pronunciation rides on the same lookup payload — no second round trip.
        val info = assertNotNull(result.pronunciation)
        assertEquals("həˈləʊ", info.variants.first().ipa)
    }

    @Test
    fun `a 404 maps to NotFound and stays inside availability=Available so aggregators trust it`() {
        val engine =
            MockEngine {
                respondError(
                    status = HttpStatusCode.NotFound,
                    content = NOT_FOUND_JSON,
                )
            }
        val adapter = adapter(engine)

        val result = runBlocking { adapter.lookup(query("plumbus")) }

        // A definitive miss from a single source is `NotFound`, not
        // `Degraded` — the aggregator must be able to weigh it as evidence,
        // not noise.
        assertEquals(VerifierAvailability.Available, result.availability)
        assertEquals(LexicalExistence.NotFound, result.existence)
    }

    @Test
    fun `a 5xx degrades instead of pretending the term does not exist`() {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val adapter = adapter(engine)

        val result = runBlocking { adapter.lookup(query("hello")) }

        assertTrue(result.availability is VerifierAvailability.Degraded)
        assertEquals(LexicalExistence.Unknown, result.existence)
    }

    @Test
    fun `a multi-word unit is rejected up front without burning a network round trip`() {
        var calls = 0
        val engine =
            MockEngine {
                calls++
                respond(content = "[]", status = HttpStatusCode.OK)
            }
        val adapter = adapter(engine)

        val result = runBlocking { adapter.lookup(query("come across")) }

        assertEquals(0, calls, "free-dictionary cannot index multi-word units; do not waste a call")
        assertTrue(result.availability is VerifierAvailability.Unavailable)
    }

    @Test
    fun `disabling network in policy short-circuits to Unavailable without contacting the service`() {
        var calls = 0
        val engine =
            MockEngine {
                calls++
                respond(content = "[]", status = HttpStatusCode.OK)
            }
        val adapter = adapter(engine)

        val offline =
            LexicalVerificationQuery(
                text = "hello",
                studyLanguageTag = "en",
                policy = VerificationPolicy(allowNetwork = false),
            )
        val result = runBlocking { adapter.lookup(offline) }

        assertEquals(0, calls, "policy.allowNetwork=false must short-circuit")
        assertTrue(result.availability is VerifierAvailability.Unavailable)
    }

    @Test
    fun `a slash in the term is percent-encoded into one segment, not leaked as a path separator`() {
        var capturedPath = ""
        val engine =
            MockEngine { request ->
                capturedPath = request.url.encodedPath
                respondError(status = HttpStatusCode.NotFound, content = NOT_FOUND_JSON)
            }
        val adapter = adapter(engine)

        runBlocking { adapter.lookup(query("a/b")) }

        // Without `encodeSlash = true` the `/` would split into `/a/b` and let
        // a crafted term rewrite the request path (traversal). It must stay a
        // single encoded segment.
        assertTrue(
            capturedPath.endsWith("/a%2Fb", ignoreCase = true),
            "slash must be percent-encoded into one segment; got: $capturedPath",
        )
    }

    private fun adapter(engine: MockEngine): FreeDictionaryLexicalAdapter =
        FreeDictionaryLexicalAdapter(
            httpClient =
                HttpClient(engine) {
                    expectSuccess = true
                    install(ContentNegotiation) { json(json) }
                },
            clock = { 0L },
            baseUrl = "https://api.dictionaryapi.dev/api/v2/entries/en",
        )

    private fun query(text: String): LexicalVerificationQuery =
        LexicalVerificationQuery(text = text, studyLanguageTag = "en")
}

private const val HELLO_SUCCESS_JSON = """
[
  {
    "word": "hello",
    "phonetic": "həˈləʊ",
    "phonetics": [
      {"text": "həˈləʊ", "audio": "https://example.test/hello-uk.mp3"},
      {"text": "həˈloʊ", "audio": ""}
    ],
    "meanings": [
      {"partOfSpeech": "noun", "definitions": [{"definition": "A greeting"}]},
      {"partOfSpeech": "verb", "definitions": [{"definition": "Say hello"}]}
    ]
  }
]
"""

private const val NOT_FOUND_JSON = """
{
  "title": "No Definitions Found",
  "message": "Sorry pal, we couldn't find definitions for the word you were looking for."
}
"""
