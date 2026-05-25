package app.sensee.grammar.data

import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarLabelForm
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.UsageLabel
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GrammarLabelsTest {
    private val labels =
        Json { ignoreUnknownKeys = true }
            .decodeFromString<GrammarTaxonomyDto>(
                GrammarTaxonomyMockFixtures().fixtures.getValue("practice/grammar/taxonomy"),
            ).toGrammarLabels()

    @Test
    fun `resolves unit type and grammar tag and usage and complement to native long labels`() {
        assertEquals("Фразовый глагол", labels.unitType(GrammarUnitType.PhrasalVerb, "ru"))
        assertEquals(
            "Неразделяемый",
            labels.form(GrammarTag(GrammarCategory.Separability, GrammarForm.Inseparable), "ru"),
        )
        assertEquals(
            "Неформальный",
            labels.usage(UsageLabel.resolve("register", "informal")!!, "ru"),
        )
        assertEquals("Инфинитив с to", labels.complement(ComplementType.ToInfinitive, "ru"))
    }

    @Test
    fun `resolves study-language short labels for sense-card badges`() {
        assertEquals(
            "phr. v.",
            labels.unitType(GrammarUnitType.PhrasalVerb, "en", GrammarLabelForm.Short),
        )
        assertEquals(
            "insep.",
            labels.form(
                tag = GrammarTag(GrammarCategory.Separability, GrammarForm.Inseparable),
                language = "en",
                form = GrammarLabelForm.Short,
            ),
        )
        assertEquals(
            "infml.",
            labels.usage(UsageLabel.resolve("register", "informal")!!, "en", GrammarLabelForm.Short),
        )
        assertEquals(
            "+to-inf",
            labels.complement(ComplementType.ToInfinitive, "en", GrammarLabelForm.Short),
        )
    }

    @Test
    fun `a missing short value falls back to the long form for the same language`() {
        // The `transitivity` category has no short label in EN (intentional —
        // the category header is rarely shown abbreviated); a Short lookup
        // therefore returns the long form, never the raw id.
        assertEquals(
            "transitivity",
            labels.category(GrammarCategory.Transitivity, "en", GrammarLabelForm.Short),
        )
    }

    @Test
    fun `an unknown language for a known id resolves to null`() {
        assertNull(labels.unitType(GrammarUnitType.PhrasalVerb, "fr"))
    }

    @Test
    fun `the empty resolver returns null instead of crashing`() {
        assertNull(GrammarLabels.EMPTY.unitType(GrammarUnitType.Idiom, "ru"))
        assertNull(GrammarLabels.EMPTY.complement(ComplementType.Gerund, "en"))
    }
}
