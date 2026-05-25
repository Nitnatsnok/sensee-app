package app.sensee.ai.fixture

import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FixtureAiEnrichmentClientTest {
    private val client = FixtureAiEnrichmentClient()

    @Test
    fun `fixture client answers available with at least one candidate`() =
        runTest {
            val result =
                client.enrich(EnrichmentRequest(term = "run"))

            assertEquals(EnrichmentAvailability.Available, result.availability)
            assertTrue(result.suggestions.isNotEmpty())
        }

    @Test
    fun `a known demo term expands into a deterministic multi-sense set`() =
        runTest {
            val first = client.enrich(EnrichmentRequest(term = "come across"))
            val second = client.enrich(EnrichmentRequest(term = "Come Across"))

            assertTrue(first.suggestions.size >= 2, "a polysemous demo term yields several senses")
            assertEquals(
                first.suggestions.map { it.translation },
                second.suggestions.map { it.translation },
                "the answer is deterministic and case-insensitive",
            )
        }

    @Test
    fun `an unknown term falls back to a single generic suggestion`() =
        runTest {
            val result = client.enrich(EnrichmentRequest(term = "zxqwerty"))

            assertEquals(1, result.suggestions.size)
        }
}
