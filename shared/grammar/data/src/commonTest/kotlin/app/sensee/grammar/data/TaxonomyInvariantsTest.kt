package app.sensee.grammar.data

import app.sensee.grammar.domain.TaxonomyInvariants
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TaxonomyInvariantsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `known unit type ids project all top-level entries from the fixture`() {
        val invariants = readFixtureInvariants()

        assertTrue("noun" in invariants.knownUnitTypeIds)
        assertTrue("verb" in invariants.knownUnitTypeIds)
        assertTrue("phrasal_verb" in invariants.knownUnitTypeIds)
        assertTrue("idiom" in invariants.knownUnitTypeIds)
    }

    @Test
    fun `known complement ids project the dictionary entries from the fixture`() {
        val invariants = readFixtureInvariants()

        assertTrue("noun" in invariants.knownComplementIds)
        assertTrue("gerund" in invariants.knownComplementIds)
        assertTrue("to_infinitive" in invariants.knownComplementIds)
        assertTrue("intransitive" in invariants.knownComplementIds)
    }

    @Test
    fun `allowed values by axis project to a per-axis set of value ids`() {
        val invariants = readFixtureInvariants()

        assertEquals(
            setOf("formal", "informal", "slang", "literary", "neutral"),
            invariants.allowedValuesByAxis.getValue("register"),
        )
        assertEquals(
            setOf("bre", "ame", "ause", "cane"),
            invariants.allowedValuesByAxis.getValue("region"),
        )
    }

    @Test
    fun `allowed forms by category project a union of forms across unit types`() {
        val invariants = readFixtureInvariants()

        assertEquals(
            setOf("infinitive", "past_tense", "past_participle", "present_participle", "gerund"),
            invariants.allowedFormsByCategory.getValue("verb"),
        )
        assertEquals(
            setOf("separable", "inseparable"),
            invariants.allowedFormsByCategory.getValue("separability"),
        )
    }

    @Test
    fun `empty invariants are the safe degraded value`() {
        assertTrue(TaxonomyInvariants.EMPTY.knownUnitTypeIds.isEmpty())
        assertTrue(TaxonomyInvariants.EMPTY.knownComplementIds.isEmpty())
        assertTrue(TaxonomyInvariants.EMPTY.allowedValuesByAxis.isEmpty())
        assertTrue(TaxonomyInvariants.EMPTY.allowedFormsByCategory.isEmpty())
    }

    private fun readFixtureInvariants(): TaxonomyInvariants {
        val fixture = GrammarTaxonomyMockFixtures().fixtures["practice/grammar/taxonomy"]
        assertNotNull(fixture)
        return json.decodeFromString<GrammarTaxonomyDto>(fixture).toTaxonomyInvariants()
    }
}
