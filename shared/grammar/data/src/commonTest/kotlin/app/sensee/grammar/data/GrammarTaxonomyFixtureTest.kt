package app.sensee.grammar.data

import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.UsageAxis
import app.sensee.grammar.domain.UsageValue
import app.sensee.grammar.domain.normalizedTaxonomyId
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The fixture is the closed set of "client-known" ids; backend additions
 * beyond it surface as `Unknown(id)` at runtime. Canon: `docs/pos-and-forms.adoc`.
 */
class GrammarTaxonomyFixtureTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `the taxonomy fixture deserializes with every known unit type`() {
        assertEquals(GrammarUnitType.knownEntries.size, readTaxonomy().unitTypes.size)
    }

    @Test
    fun `unit type ids map to a known sealed case`() {
        val knownIds = GrammarUnitType.knownEntries.map { it.id.normalizedTaxonomyId() }.toSet()

        readTaxonomy().unitTypes.forEach { unitType ->
            assertTrue(
                unitType.id.normalizedTaxonomyId() in knownIds,
                "Missing known unit type for ${unitType.id}",
            )
        }
    }

    @Test
    fun `categories and forms in the fixture map to known sealed cases`() {
        val categoryIds = GrammarCategory.knownEntries.map { it.id }.toSet()
        val formIds = GrammarForm.knownEntries.map { it.id }.toSet()

        readTaxonomy().unitTypes.forEach { unitType ->
            unitType.categories.forEach { category ->
                assertTrue(category.id in categoryIds, "Unknown category ${category.id}")
                category.forms.forEach { form ->
                    assertTrue(form.id in formIds, "Unknown form ${category.id}/${form.id}")
                }
            }
        }
    }

    @Test
    fun `usage axes and values in the fixture map to known sealed cases`() {
        val taxonomy = readTaxonomy()
        val axisIds = UsageAxis.knownEntries.map { it.id }.toSet()
        val valueIds = UsageValue.knownEntries.map { it.id }.toSet()

        assertEquals(UsageAxis.knownEntries.size, taxonomy.usageAxes.size)
        taxonomy.usageAxes.forEach { axis ->
            assertTrue(axis.id in axisIds, "Unknown usage axis ${axis.id}")
            axis.values.forEach { value ->
                assertTrue(value.id in valueIds, "Unknown usage value ${axis.id}/${value.id}")
            }
        }
    }

    @Test
    fun `complement types cover the known sealed cases`() {
        val taxonomy = readTaxonomy()

        assertEquals(
            ComplementType.knownEntries.map { it.id }.toSet(),
            taxonomy.complementTypes.map { it.id }.toSet(),
        )
    }

    @Test
    fun `ids are unique within their parent`() {
        val unitTypes = readTaxonomy().unitTypes

        val unitIds = unitTypes.map { it.id }
        assertEquals(unitIds.size, unitIds.toSet().size)
        unitTypes.forEach { unitType ->
            val categoryIds = unitType.categories.map { it.id }
            assertEquals(categoryIds.size, categoryIds.toSet().size)
        }
    }

    private fun readTaxonomy(): GrammarTaxonomyDto {
        val fixture = GrammarTaxonomyMockFixtures().fixtures["practice/grammar/taxonomy"]
        assertNotNull(fixture)
        return json.decodeFromString<GrammarTaxonomyDto>(fixture)
    }
}
