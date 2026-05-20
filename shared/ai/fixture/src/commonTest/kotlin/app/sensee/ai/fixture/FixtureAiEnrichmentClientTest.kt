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
}
