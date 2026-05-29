package app.sensee.verification.senseeCurated.remote

import app.sensee.verification.senseeCurated.CefrCatalogDto
import app.sensee.verification.senseeCurated.FamilyCatalogDto
import app.sensee.verification.senseeCurated.FrequencyCatalogDto
import app.sensee.verification.senseeCurated.SenseCatalogDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue

class SenseeCuratedMockFixturesTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val fixtures = SenseeCuratedMockFixtures().fixtures

    @Test
    fun `every facet catalog is present non empty and lemma keyed in lower case`() {
        val frequency =
            json.decodeFromString(FrequencyCatalogDto.serializer(), fixtures.getValue("verification/frequency"))
        val cefr =
            json.decodeFromString(CefrCatalogDto.serializer(), fixtures.getValue("verification/cefr"))
        val senses =
            json.decodeFromString(SenseCatalogDto.serializer(), fixtures.getValue("verification/senses"))
        val family =
            json.decodeFromString(FamilyCatalogDto.serializer(), fixtures.getValue("verification/family"))

        assertTrue(frequency.lemmas.isNotEmpty(), "frequency catalog must not be empty")
        assertTrue(cefr.lemmas.isNotEmpty(), "cefr catalog must not be empty")
        assertTrue(senses.lemmas.isNotEmpty(), "senses catalog must not be empty")
        assertTrue(family.families.isNotEmpty(), "family catalog must not be empty")

        // The runtime looks lemmas up by `term.trim().lowercase()`, so keys must
        // already be lowercased or they would be permanently unmatchable.
        assertTrue(frequency.lemmas.keys.all { it == it.lowercase() }, "frequency keys must be lowercased")
        assertTrue(cefr.lemmas.keys.all { it == it.lowercase() }, "cefr keys must be lowercased")
        assertTrue(senses.lemmas.keys.all { it == it.lowercase() }, "sense keys must be lowercased")
        assertTrue(family.families.keys.all { it == it.lowercase() }, "family keys must be lowercased")
    }
}
