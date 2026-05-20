package app.sensee.grammar.data

import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.UsageAxis
import app.sensee.grammar.domain.UsageValue
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GrammarTaxonomyFixtureTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `the taxonomy fixture deserializes with every unit type`() {
        assertEquals(GrammarUnitType.entries.size, readTaxonomy().unitTypes.size)
    }

    @Test
    fun `unit type ids map to the domain enum`() {
        val domainIds = GrammarUnitType.entries.map { it.name.normalizedId() }.toSet()

        readTaxonomy().unitTypes.forEach { unitType ->
            assertTrue(
                unitType.id.normalizedId() in domainIds,
                "Missing domain unit type for ${unitType.id}",
            )
        }
    }

    @Test
    fun `categories and forms map to the domain invariant`() {
        val categoryIds = GrammarCategory.entries.map { it.id }.toSet()
        val formIds = GrammarForm.entries.map { it.id }.toSet()

        readTaxonomy().unitTypes.forEach { unitType ->
            unitType.categories.forEach { category ->
                assertTrue(category.id in categoryIds, "Unknown category ${category.id}")
                category.forms.forEach { form ->
                    assertTrue(form.id in formIds, "Unknown form ${category.id}/${form.id}")
                    assertNotNull(
                        GrammarTag.resolve(category.id, form.id),
                        "Invalid grammar pair ${category.id}/${form.id}",
                    )
                }
            }
        }
    }

    @Test
    fun `usage axes and values map to the domain enum`() {
        val taxonomy = readTaxonomy()
        val axisIds = UsageAxis.entries.map { it.id }.toSet()
        val valueIds = UsageValue.entries.map { it.id }.toSet()

        assertEquals(UsageAxis.entries.size, taxonomy.usageAxes.size)
        taxonomy.usageAxes.forEach { axis ->
            assertTrue(axis.id in axisIds, "Unknown usage axis ${axis.id}")
            axis.values.forEach { value ->
                assertTrue(value.id in valueIds, "Unknown usage value ${axis.id}/${value.id}")
            }
        }
    }

    @Test
    fun `complement types cover the domain enum`() {
        val taxonomy = readTaxonomy()

        assertEquals(
            ComplementType.entries.map { it.id }.toSet(),
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

private fun String.normalizedId(): String = filter { it.isLetterOrDigit() }.lowercase()
